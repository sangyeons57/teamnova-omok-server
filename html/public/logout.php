<?php
/** 로그아웃 기능
 * AccessToken 제거 (클라에서 알아서 작업)
 * RefreshToken 사용 처리
 */

$container = new Container();
(new AppProvider())->register($container);
(new ResponseProvider())->register($container);
(new GuardProvider())->register($container);

$responseService = $container->get(ResponseService::class);

$guard = $container->get(AccessTokenGuardService::class)->requirePayload();;

$userId = isset($payload['sub']) ? (string)$payload['sub'] : '';
$userService = $container->get(UserService::class);
if ($userId === '' || !$userService->isActive($userId)) {
    $responseService->error('USER_NOT_ACTIVE', 403, '사용자 상태가 활성화되어 있지 않습니다.');
}

