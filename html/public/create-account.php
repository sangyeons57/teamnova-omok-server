<?php
/**
 * 신규 계정 생성
 * 절차
 * - DB에 사용자 생성(users + auth_providers)
 * - (옵션) Access 토큰 발급(JWT, HS256)
 * - (옵션) Refresh 토큰 발급 + 저장(refresh_tokens)
 *
 * 실제로 호출되는 외부 API에서 처리(terms 조회 + user_terms_acceptances 3건 입력)
 * 여기서는 계정 생성만(필수) 하고 로그인을 따로 요청.
 *
 * 요청 파라미터(JSON, POST):
 * - provider: "GUEST" | "GOOGLE"
 * - provider_id_token: 클라이언트에서 받은 Google ID 토큰(GOOGLE일 때 필수)
 * - display_name: auto-generated (user-XXXXXXXXXXXX)
 * - profile_icon_code: default '0'
 * - tokens: always issued immediately
 *
 * 응답:
 * - 201/200: Envelope(meta, data{type: account_create, id: user_id, payload: {...}}, error: null)
 * - 400/405/500: Envelope(meta, data: null, error: {...})
 */

require_once __DIR__ . '/../shared/Container/ContainerFactory.php';

// 서비스 로더 초기화 및 컨테이너 준비
$container = ContainerFactory::create();

/** @var ResponseService $responseService */
$responseService = $container->get(ResponseService::class);
/** @var RequestService $requestService */
$requestService = $container->get(RequestService::class);
/** @var ValidationService $validator */
$validator = $container->get(ValidationService::class);
/** @var UuidService $uuidService */
$uuidService = $container->get(UuidService::class);

// 요청 본문
$body = $requestService->readBody('POST');

// 파라미터 파싱
$provider = isset($body['provider']) ? trim($body['provider']) : '';
$providerIdToken = isset($body['provider_id_token']) ? trim($body['provider_id_token']) : '';
$provider_user_id = null;
$display_name = 'user-' . substr(str_replace('-', '', $uuidService->v4()), 0, 12);
$profile_icon_code = '0';

// 유효성 검사
$allowed_providers = array('GUEST', 'GOOGLE');
if ($provider === '' || !$validator->inArrayStrict($provider, $allowed_providers)) {
    $responseService->error('INVALID_PROVIDER', 400, 'provider는 GUEST 또는 GOOGLE 이어야 합니다.');
}

if ($provider === 'GOOGLE') {
    if ($providerIdToken === '') {
        $responseService->error('INVALID_PROVIDER_ID_TOKEN', 400, 'provider_id_token이 비어 있습니다.');
    }

    try {
        /** @var GoogleClientService $googleClient */
        $googleClient = $container->get(GoogleClientService::class);
        $responseService->success(201, array('message' => '계정 생성 성공'. Config::webClientId()) );
        $provider_user_id = $googleClient->getUserId($providerIdToken);
    } catch (RuntimeException $e) {
        $responseService->error('INVALID_GOOGLE_ID_TOKEN', 401, '유효하지 않은 Google ID 토큰입니다.');
    } catch (Exception $e) {
        $responseService->exceptionInternal('Google ID 토큰 검증 중 오류가 발생했습니다.', $e, 'GOOGLE_ID_TOKEN_VERIFY_FAILED');
    }

    if ($provider_user_id === '' || $validator->mbLength($provider_user_id) > 255) {
        $responseService->error('INVALID_PROVIDER_USER_ID', 400, 'Google 사용자 식별자가 유효하지 않습니다.');
    }
} else if ($provider === 'GUEST') {
    // GUEST는 provider_user_id 미사용: 항상 NULL 유지
    $provider_user_id = null;
}

// 트랜잭션 처리
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

    $responseService->success($result['created'] ? 201 : 200, $payloadOut);

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
        $responseService->exceptionInternal('auth_providers와 users의 무결성이 맞지 않습니다.', $e, 'DATA_INTEGRITY_ERROR');
    }
    $responseService->exceptionInternal('알 수 없는 오류가 발생했습니다.', $e);
}