<?php
/**
 * Google 계정 연결 엔드포인트
 * - Access Token 검증 후 현재 로그인한 사용자를 식별
 * - Google ID 토큰을 검증하여 provider_user_id(sub)을 추출
 * - auth_providers 테이블에서 해당 사용자/GOOGLE 레코드의 provider_user_id를 갱신하고 linked_at을 현재 시각으로 업데이트
 * - 이미 동일한 provider_user_id가 존재하더라도 linked_at이 최신인 레코드가 우선되도록 처리
 */

require_once __DIR__ . '/../shared/Constants/UserStatus.php';
require_once __DIR__ . '/../shared/Container/ContainerFactory.php';

$container = ContainerFactory::create();
/** @var ResponseService $response */
$response = $container->get(ResponseService::class);
/** @var RequestService $request */
$request = $container->get(RequestService::class);
/** @var AccessTokenGuardService $guard */
$guard = $container->get(AccessTokenGuardService::class);
/** @var ValidationService $validator */
$validator = $container->get(ValidationService::class);
/** @var UserService $userService */
$userService = $container->get(UserService::class);
/** @var AuthProviderRepository $authProviders */
$authProviders = $container->get(AuthProviderRepository::class);

$payload = $guard->requirePayload();
$userId = isset($payload['sub']) ? (string)$payload['sub'] : '';
if ($userId === '') {
    $response->error('ACCESS_TOKEN_INVALID', 401, '유효한 사용자 식별자를 확인할 수 없습니다.');
}

$user = $userService->findById($userId);
if ($user === null) {
    $response->error('USER_NOT_FOUND', 404, '사용자를 찾을 수 없습니다.');
}

$status = isset($user['status']) ? (string)$user['status'] : '';
if ($status !== UserStatus::ACTIVE) {
    $response->error('USER_NOT_ACTIVE', 403, '사용자 상태가 활성화되어 있지 않습니다.');
}

$body = $request->readBody('POST');
if (!isset($body['provider_id_token']) || !is_string($body['provider_id_token'])) {
    $response->error('INVALID_PROVIDER_ID_TOKEN', 400, 'Google ID 토큰(provider_id_token)을 문자열로 전달해 주세요.');
}

$providerIdToken = trim($body['provider_id_token']);
if ($providerIdToken === '') {
    $response->error('INVALID_PROVIDER_ID_TOKEN', 400, 'Google ID 토큰(provider_id_token)이 비어 있습니다.');
}

try {
    /** @var GoogleClientService $googleClient */
    $googleClient = $container->get(GoogleClientService::class);
    $providerUserId = $googleClient->getUserId($providerIdToken);
} catch (RuntimeException $e) {
    $response->error('INVALID_GOOGLE_ID_TOKEN', 401, '유효하지 않은 Google ID 토큰입니다.');
} catch (Exception $e) {
    $response->exceptionInternal('Google ID 토큰 검증 중 오류가 발생했습니다.', $e, 'GOOGLE_ID_TOKEN_VERIFY_FAILED');
}

if ($providerUserId === '' || $validator->mbLength($providerUserId) > 255) {
    $response->error('INVALID_PROVIDER_USER_ID', 400, 'Google 사용자 식별자가 유효하지 않습니다.');
}

$previous = $authProviders->findByUserIdAndProvider($userId, 'GOOGLE');
$previousProviderUserId = null;
if ($previous !== null && isset($previous['provider_user_id'])) {
    $previousProviderUserId = $previous['provider_user_id'];
}

try {
    $authProviders->upsertProviderUserId($userId, 'GOOGLE', $providerUserId);
    $updated = $authProviders->findByUserIdAndProvider($userId, 'GOOGLE');
} catch (PDOException $e) {
    $response->exceptionDbError('Google 계정 연결 중 데이터베이스 오류가 발생했습니다.', $e);
}

$linkedAt = null;
if ($updated !== null && isset($updated['linked_at'])) {
    $linkedAt = $updated['linked_at'];
}

$response->success(200, array(
    'linked' => true,
    'provider' => 'GOOGLE',
    'user_id' => $userId,
    'provider_user_id' => $providerUserId,
    'previous_provider_user_id' => $previousProviderUserId,
    'linked_at' => $linkedAt,
));
