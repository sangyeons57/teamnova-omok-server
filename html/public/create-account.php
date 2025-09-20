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
 * 응답:
 * - 201/200: Envelope(meta, data{type: account_create, id: user_id, payload: {...}}, error: null)
 * - 400/405/500: Envelope(meta, data: null, error: {...})
 */

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
/** @var UuidService $uuidService */
$uuidService = $container->get(UuidService::class);

// 공통 시작부
$envelope = $requestService->readEnvelope('POST');
$body = $envelope['body'];

// 파라미터 파싱
$provider = isset($body['provider']) ? trim($body['provider']) : '';
$provider_user_id = isset($body['provider_user_id']) ? trim($body['provider_user_id']) : '';
$display_name = 'user-' . substr(str_replace('-', '', $uuidService->v4()), 0, 12);
$profile_icon_code = '0';

// 유효성 검증
$allowed_providers = array('GUEST', 'GOOGLE');
if ($provider === '' || !$validator->inArrayStrict($provider, $allowed_providers)) {
    $responseService->error('INVALID_PROVIDER', 400, 'provider는 GUEST 또는 GOOGLE 이어야 합니다.');
}

if ($provider === 'GOOGLE') {
    // GOOGLE은 provider_user_id 필수
    if ($provider_user_id === '' || $validator->mbLength($provider_user_id) > 255) {
        $responseService->error('INVALID_PROVIDER_USER_ID', 400, 'provider_user_id가 비어있거나 길이가 너무 깁니다(최대 255).');
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

    /** @var TokenService $tokenService */
    $tokenService = $container->get(TokenService::class);
    $tokens = $tokenService->issue($result['user']['user_id']);

    $payloadOut = array(
        'created' => $result['created'],
        'user_id' => $result['user']['user_id'],
    );
    $payloadOut = array_merge($payloadOut, $tokens);

    $responseService->success($result['created'] ? 201 : 200, 'account_create', $result['user']['user_id'], $payloadOut);

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
