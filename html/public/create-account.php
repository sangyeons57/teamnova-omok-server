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
 * - display_name: auto-generated (user-XXXXXXXXXXXX)
 * - profile_icon_code: default '0'
 * - tokens: always issued immediately
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

use Cassandra\Uuid;

require_once __DIR__ . '/../shared/Config.php';
require_once __DIR__ . '/../shared/Database.php';
require_once __DIR__ . '/../shared/Services/Auth/JwtService.php';
require_once __DIR__ . '/../shared/Services/Auth/TokenService.php';
require_once __DIR__ . '/../shared/Repositories/UserRepository.php';
require_once __DIR__ . '/../shared/Repositories/AuthProviderRepository.php';
require_once __DIR__ . '/../shared/Repositories/RefreshTokenRepository.php';
require_once __DIR__ . '/../shared/Services/AccountService.php';
require_once __DIR__ . '/../shared/Container/Container.php';
require_once __DIR__ . '/../shared/Container/ServiceProvider.php';
require_once __DIR__ . '/../shared/Container/AppProvider.php';
require_once __DIR__ . '/../shared/Container/UtilProvider.php';
require_once __DIR__ . '/../shared/Container/AuthProvider.php';
require_once __DIR__ . '/../shared/Container/ResponseProvider.php';
require_once __DIR__ . '/../shared/Container/RequestProvider.php';

// 컨테이너 초기화 및 프로바이더 등록
$container = new Container();
(new AppProvider())->register($container);
(new UtilProvider())->register($container);
(new AuthProvider())->register($container);
(new ResponseProvider())->register($container);
(new RequestProvider())->register($container);

/** @var ResponseService $responseService */
$responseService = $container->get(ResponseService::class);
/** @var RequestService $requestService */
$requestService = $container->get(RequestService::class);
/** @var ValidationService $validator */
$validator = $container->get(ValidationService::class);

// 공통 시작부
$responseService->setJsonResponseHeader();
$requestService->assertMethod('POST');
$body = $requestService->readJsonBody();

// 파라미터 파싱
$provider = isset($body['provider']) ? trim($body['provider']) : '';
$provider_user_id = isset($body['provider_user_id']) ? trim($body['provider_user_id']) : '';
$display_name = 'user-' . substr(str_replace('-', '', Uuid::v4()), 0, 12);
$profile_icon_code = '0';

// 유효성 검증
$allowed_providers = array('GUEST', 'GOOGLE');
if ($provider === '' || !$validator->inArrayStrict($provider, $allowed_providers)) {
    $responseService->json(400, array(
        'success' => false,
        'error' => 'INVALID_PROVIDER',
        'message' => 'provider는 GUEST 또는 GOOGLE 이어야 합니다.'
    ));
}

if ($provider === 'GOOGLE') {
    // GOOGLE은 provider_user_id 필수
    if ($provider_user_id === '' || $validator->mbLength($provider_user_id) > 255) {
        $responseService->json(400, array(
            'success' => false,
            'error' => 'INVALID_PROVIDER_USER_ID',
            'message' => 'provider_user_id가 비어있거나 길이가 너무 깁니다(최대 255).'
        ));
    }
} else if ($provider === 'GUEST') {
    // GUEST는 provider_user_id 미사용: 항상 NULL로 강제
    $provider_user_id = null;
}

// 비즈니스 로직
$pdo = null;
try {
    /** @var PDO $pdo */
    $pdo = $container->get(PDO::class);

    /** @var AccountService $account */
    $account = $container->get(AccountService::class);
    $result = $account->createOrGetUser($provider, $provider_user_id, $display_name, $profile_icon_code);

    $responsePayload = array(
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

    /** @var TokenService $tokenService */
    $tokenService = $container->get(TokenService::class);
    $tokens = $tokenService->issue($result['user']['user_id']);
    $responsePayload = array_merge($responsePayload, $tokens);

    $responseService->json($result['created'] ? 201 : 200, $responsePayload);

} catch (PDOException $e) {
    if ($pdo instanceof PDO && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    $responseService->exceptionDbError('데이터베이스 오류가 발생했습니다.', $e);
} catch (Exception $e) {
    if ($pdo instanceof PDO && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    if ($e->getMessage() === 'DATA_INTEGRITY_ERROR') {
        $responseService->exceptionInternal('auth_providers는 있으나 users가 없습니다.', $e, 'DATA_INTEGRITY_ERROR');
    }
    $responseService->exceptionInternal('내부 오류가 발생했습니다.', $e);
}
