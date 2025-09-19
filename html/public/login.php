<?php
/**
 * 로그인 상태 확인(Access Token 검증)
 * 요청(JSON, POST):
 * - access_token: string (Authorization 헤더를 사용할 경우 생략 가능)
 */

require_once __DIR__ . '/../shared/Container/Container.php';
require_once __DIR__ . '/../shared/Container/ServiceProvider.php';
require_once __DIR__ . '/../shared/Container/AppProvider.php';
require_once __DIR__ . '/../shared/Container/UtilProvider.php';
require_once __DIR__ . '/../shared/Container/AuthProvider.php';
require_once __DIR__ . '/../shared/Container/ResponseProvider.php';
require_once __DIR__ . '/../shared/Container/RequestProvider.php';
require_once __DIR__ . '/../shared/Container/GuardProvider.php';

// 컨테이너 초기화 및 가드/요청/응답 서비스
$container = new Container();
(new AppProvider())->register($container);
(new UtilProvider())->register($container);
(new AuthProvider())->register($container);
(new ResponseProvider())->register($container);
(new RequestProvider())->register($container);
(new GuardProvider())->register($container);
/** @var ResponseService $responseService */
$responseService = $container->get(ResponseService::class);
/** @var RequestService $requestService */
$requestService = $container->get(RequestService::class);

$requestService->assertMethod('POST');
$body = $requestService->readJsonBody();
/** @var AccessTokenGuardService $guard */
$guard = $container->get(AccessTokenGuardService::class);
$payload = $guard->requirePayload($body);

$userId = isset($payload['sub']) ? (string)$payload['sub'] : '';

$payloadOut = array(
    'user_id' => $userId,
);

if (isset($payload['role'])) {
    $payloadOut['role'] = $payload['role'];
}

if (isset($payload['exp']) && is_numeric($payload['exp'])) {
    $payloadOut['expires_at'] = (int)$payload['exp'];
}

$responseService->success(200, 'login_status', $userId, $payloadOut);
