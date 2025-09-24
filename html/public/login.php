<?php
/**
 * 로그인 상태 확인(Access Token 검증)
 * 요청(JSON, POST):
 * - Authorization 헤더: Bearer <access_token>
 */

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
if ($userId === '' || !$userService->isActive($userId)) {
    $responseService->error('USER_NOT_ACTIVE', 403, '사용자 상태가 활성화되어 있지 않습니다.');
}

$payloadOut = array(
    'user_id' => $userId,
);

if (isset($payload['role'])) {
    $payloadOut['role'] = $payload['role'];
}

if (isset($payload['exp']) && is_numeric($payload['exp'])) {
    $payloadOut['expires_at'] = (int)$payload['exp'];
}

$responseService->success(200, 'login', $userId, $payloadOut);
