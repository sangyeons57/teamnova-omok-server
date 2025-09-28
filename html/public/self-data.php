<?php
/**
 * 자기 자신 정보 조회 엔드포인트
 * - Access Token 검증
 * - 활성 사용자만 조회 가능
 */

require_once __DIR__ . '/../shared/Constants/UserStatus.php';
require_once __DIR__ . '/../shared/Container/ContainerFactory.php';

$container = ContainerFactory::create();
/** @var ResponseService $response */
$response = $container->get(ResponseService::class);
/** @var AccessTokenGuardService $guard */
$guard = $container->get(AccessTokenGuardService::class);
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

$profileIcon = isset($user['profile_icon_code']) ? (string)$user['profile_icon_code'] : null;
$score = isset($user['score']) ? (int)$user['score'] : 0;

$authProvider = $authProviders->findByUserId($userId);
$authProviderPayload = null;
if ($authProvider !== null) {
    $authProviderPayload = array(
        'provider' => isset($authProvider['provider']) ? (string)$authProvider['provider'] : null,
        'provider_user_id' => isset($authProvider['provider_user_id']) ? $authProvider['provider_user_id'] : null,
    );
    if (isset($authProvider['linked_at'])) {
        $authProviderPayload['linked_at'] = $authProvider['linked_at'];
    }
}

$response->success(200, array(
    'user' => array(
        'user_id' => (string)$user['user_id'],
        'display_name' => isset($user['display_name']) ? (string)$user['display_name'] : '',
        'profile_icon_code' => $profileIcon,
        'role' => isset($user['role']) ? (string)$user['role'] : null,
        'status' => $status,
        'score' => $score,
    ),
    'provider' => $authProviderPayload,
));
