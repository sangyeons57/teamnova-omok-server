<?php
/**
 * 회원 탈퇴(비활성화) 처리 엔드포인트
 * - Access Token으로 인증된 사용자를 INACTIVE 상태로 전환
 * - 모든 Refresh Token 무효화
 */

require_once __DIR__ . '/../shared/Constants/UserStatus.php';
require_once __DIR__ . '/../shared/Container/ContainerFactory.php';

$container = ContainerFactory::create();
/** @var ResponseService $response */
$response = $container->get(ResponseService::class);
/** @var AccessTokenGuardService $guard */
$guard = $container->get(AccessTokenGuardService::class);
$payload = $guard->requirePayload();

$userId = isset($payload['sub']) ? (string)$payload['sub'] : '';
if ($userId === '') {
    $response->error('ACCESS_TOKEN_INVALID', 401, '유효한 사용자 식별자를 확인할 수 없습니다.');
}

/** @var UserService $userService */
$userService = $container->get(UserService::class);
$user = $userService->findById($userId);
if ($user === null) {
    $response->error('USER_NOT_FOUND', 404, '사용자를 찾을 수 없습니다.');
}

$wasActive = isset($user['status']) && (string)$user['status'] === UserStatus::ACTIVE;
$deactivated = $userService->deactivate($userId);
if (!$deactivated) {
    $response->error('USER_DEACTIVATE_FAILED', 500, '사용자 비활성화 처리에 실패했습니다.');
}

/** @var TokenService $tokenService */
$tokenService = $container->get(TokenService::class);
$tokenService->revokeAllByUserId($userId);

$response->success(200, array(
    'deactivated' => true,
    'already_inactive' => !$wasActive,
    'refresh_tokens_revoked' => true,
));
