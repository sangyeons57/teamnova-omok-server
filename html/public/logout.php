<?php
/** 로그아웃 기능
 * AccessToken 제거 (클라에서 알아서 작업)
 * RefreshToken 사용 처리
 */

require_once __DIR__ . '/../shared/Container/ContainerFactory.php';

$container = ContainerFactory::create();

/** @var ResponseService $responseService */
$responseService = $container->get(ResponseService::class);

/** @var AccessTokenGuardService $guard */
$guard = $container->get(AccessTokenGuardService::class);
$payload = $guard->requirePayload();

$userId = isset($payload['sub']) ? (string)$payload['sub'] : '';
/** @var UserService $userService */
$userService = $container->get(UserService::class);
if ($userId === '' || !$userService->isActive($userId)) {
    $responseService->error('USER_NOT_ACTIVE', 403, '사용자 상태가 활성화되어 있지 않습니다.');
}

/** @var AuthProviderRepository $authProviders */
$authProviders = $container->get(AuthProviderRepository::class);
$providerRow = $authProviders->findByUserId($userId);
$isGuest = $providerRow !== null && isset($providerRow['provider']) && (string)$providerRow['provider'] === 'GUEST';

$guestDeactivated = false;
if ($isGuest) {
    $guestDeactivated = $userService->deactivate($userId);
    if (!$guestDeactivated) {
        $responseService->error('USER_GUEST_DEACTIVATE_FAILED', 500, '게스트 계정을 비활성화하지 못했습니다.');
    }
}

/** @var TokenService $tokenService */
$tokenService = $container->get(TokenService::class);
$tokenService->revokeAllByUserId($userId);

$responseService->success(200, array(
    'refresh_tokens_revoked' => true,
    'guest_account_deactivated' => $guestDeactivated,
));
