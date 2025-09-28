<?php
/**
 * 로그인 상태 확인(Access Token 검증)
 * 요청(JSON, POST):
 * - Authorization 헤더: Bearer <access_token>
 */

require_once __DIR__ . '/../shared/Constants/UserStatus.php';
require_once __DIR__ . '/../shared/Container/ContainerFactory.php';

// 컨테이너 초기화 및 가드/응답 서비스
$container = ContainerFactory::create();
/** @var ResponseService $responseService */
$responseService = $container->get(ResponseService::class);
// 공통 시작부
/** @var AccessTokenGuardService $guard */
$guard = $container->get(AccessTokenGuardService::class);
$payload = $guard->requirePayload();

$userId = isset($payload['sub']) ? (string)$payload['sub'] : '';

/** @var UserService $userService */
$userService = $container->get(UserService::class);
/** @var AuthProviderRepository $authProviders */
$authProviders = $container->get(AuthProviderRepository::class);
if ($userId === '') {
    $responseService->error('ACCESS_TOKEN_INVALID', 401, '유효한 사용자 식별자를 확인할 수 없습니다.');
}

$user = $userService->findById($userId);
if ($user === null) {
    $responseService->error('USER_NOT_FOUND', 404, '사용자를 찾을 수 없습니다.');
}

$status = isset($user['status']) ? (string)$user['status'] : '';
if ($status !== UserStatus::ACTIVE) {
    $responseService->error('USER_NOT_ACTIVE', 403, '사용자 상태가 활성화되어 있지 않습니다.');
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

$payloadOut = array(
    'user' => array(
        'user_id' => (string)$user['user_id'],
        'display_name' => isset($user['display_name']) ? (string)$user['display_name'] : '',
        'profile_icon_code' => $profileIcon,
        'role' => isset($user['role']) ? (string)$user['role'] : null,
        'status' => $status,
        'score' => $score,
    ),
    'provider' => $authProviderPayload,
);


if (isset($payload['exp']) && is_numeric($payload['exp'])) {
    $payloadOut['expires_at'] = (int)$payload['exp'];
}

$responseService->success(200, $payloadOut);
