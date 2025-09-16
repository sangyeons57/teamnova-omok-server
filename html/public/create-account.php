<?php
/**
 * 계정 생성 기능
 * 결과
 * - DB에 계정 생성(users + auth_providers)
 * - (옵션) Access 토큰 발급(JWT, HS256)
 * - (옵션) Refresh 토큰 발급 + 저장(refresh_tokens)
 *
 * 약관 동의는 별도 API에서 처리(terms 조회 + user_terms_acceptances 3건 기록)
 * 여기서는 계정 생성과(필요시) 즉시 로그인만 다룬다.
 *
 * 요구 파라미터(JSON, POST):
 * - provider: "GUEST" | "GOOGLE"
 * - provider_user_id: 문자열(디바이스ID/랜덤키 또는 Google OAuth sub)
 * - display_name: 표시명
 * - profile_icon_code: 선택
 * - issue_tokens: true/false (기본 true) — true면 가입 후 즉시 로그인 처리
 *
 * 환경 변수:
 * - DB_HOST, DB_NAME, DB_USER, DB_PASS
 * - JWT_SECRET
 *
 * 응답:
 * - 201 Created: 새로 생성, { created: true, user_id, access_token?, refresh_token?, expires_in? }
 * - 200 OK: 이미 존재(멱등), { created: false, ... }
 * - 400/405/500: 에러
 */

require_once __DIR__ . '/../shared/Config.php';
require_once __DIR__ . '/../shared/Database.php';
require_once __DIR__ . '/../shared/Http.php';
require_once __DIR__ . '/../shared/Validation.php';
require_once __DIR__ . '/../shared/Util/Crypto.php';
require_once __DIR__ . '/../shared/Util/Uuid.php';
require_once __DIR__ . '/../shared/Util/Clock.php';
require_once __DIR__ . '/../shared/Security/Jwt.php';
require_once __DIR__ . '/../shared/Security/TokenService.php';
require_once __DIR__ . '/../shared/Repositories/UserRepository.php';
require_once __DIR__ . '/../shared/Repositories/AuthProviderRepository.php';
require_once __DIR__ . '/../shared/Repositories/RefreshTokenRepository.php';
require_once __DIR__ . '/../shared/Services/AccountService.php';

// 공통 시작부
Http::setJsonResponseHeader();
Http::assertMethod('POST');
$body = Http::readJsonBody();

// 파라미터 파싱
$provider = isset($body['provider']) ? trim($body['provider']) : '';
$provider_user_id = isset($body['provider_user_id']) ? trim($body['provider_user_id']) : '';
$display_name = isset($body['display_name']) ? trim($body['display_name']) : '';
$profile_icon_code = isset($body['profile_icon_code']) ? trim($body['profile_icon_code']) : null;
$issue_tokens = isset($body['issue_tokens']) ? (bool)$body['issue_tokens'] : true;

// 유효성 검증
$allowed_providers = array('GUEST', 'GOOGLE');
if ($provider === '' || !Validation::inArrayStrict($provider, $allowed_providers)) {
    Http::json(400, array(
        'success' => false,
        'error' => 'INVALID_PROVIDER',
        'message' => 'provider는 GUEST 또는 GOOGLE 이어야 합니다.'
    ));
}

if ($provider === 'GOOGLE') {
    // GOOGLE은 provider_user_id 필수
    if ($provider_user_id === '' || Validation::mbLength($provider_user_id) > 255) {
        Http::json(400, array(
            'success' => false,
            'error' => 'INVALID_PROVIDER_USER_ID',
            'message' => 'provider_user_id가 비어있거나 길이가 너무 깁니다(최대 255).'
        ));
    }
} else if ($provider === 'GUEST') {
    // GUEST는 provider_user_id 미사용: 항상 NULL로 강제
    $provider_user_id = null;
}

if ($display_name === '' || Validation::mbLength($display_name) > 30) {
    Http::json(400, array(
        'success' => false,
        'error' => 'INVALID_DISPLAY_NAME',
        'message' => 'display_name은 1~30자여야 합니다.'
    ));
}

if ($profile_icon_code !== null && $profile_icon_code !== '' && Validation::mbLength($profile_icon_code) > 64) {
    Http::json(400, array(
        'success' => false,
        'error' => 'INVALID_PROFILE_ICON_CODE',
        'message' => 'profile_icon_code 길이가 너무 깁니다(최대 64).'
    ));
}

// 비즈니스 로직
$pdo = null;
try {
    $pdo = Database::pdo();

    Http::json( 200, "test");

    $account = new AccountService($pdo);
    $result = $account->createOrGetUser($provider, $provider_user_id, $display_name, $profile_icon_code);

    $response = array(
        'success' => true,
        'created' => $result['created'],
        'user' => array(
            'user_id' => $result['user']['user_id'],
            'display_name' => $result['user']['display_name'],
            'profile_icon_code' => $result['user']['profile_icon_code'],
            'role' => $result['user']['role'],
            'status' => $result['user']['status'],
            'score' => intval($result['user']['score']),
        )
    );

    if ($issue_tokens) {
        $refreshRepo = new RefreshTokenRepository($pdo);
        $tokens = (new TokenService($refreshRepo))->issue(
            $result['user']['user_id'],
            $provider,
            $result['user']['role']
        );
        $response = array_merge($response, $tokens);
    }

    Http::json($result['created'] ? 201 : 200, $response);

} catch (PDOException $e) {
    if ($pdo && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    Http::exceptionDbError('데이터베이스 오류가 발생했습니다.');
} catch (Exception $e) {
    if ($pdo && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    if ($e->getMessage() === 'DATA_INTEGRITY_ERROR') {
        Http::exception(500, 'DATA_INTEGRITY_ERROR', 'auth_providers는 있으나 users가 없습니다.');
    }
    Http::exceptionInternal('내부 오류가 발생했습니다.');
}

