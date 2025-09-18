<?php
/**
 * 로그인 상태 확인(Access Token 검증)
 * 요청(JSON, POST):
 * - access_token: string (Authorization 헤더를 사용할 경우 생략 가능)
 *
 * 응답:
 * - 200 OK: { success: true, user_id, role?, expires_at? }
 * - 400/401/405/500: 에러
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
$responseService->setJsonResponseHeader();
$requestService->assertMethod('POST');
$body = $requestService->readJsonBody();
/** @var AccessTokenGuardService $guard */
$guard = $container->get(AccessTokenGuardService::class);
$payload = $guard->requirePayload($body);

$userId = isset($payload['sub']) ? (string)$payload['sub'] : '';

$responsePayload = array(
    'success' => true,
    'user_id' => $userId,
);

if (isset($payload['role'])) {
    $responsePayload['role'] = $payload['role'];
}

if (isset($payload['exp']) && is_numeric($payload['exp'])) {
    $responsePayload['expires_at'] = (int)$payload['exp'];
}

$responseService->json(200, $responsePayload);
