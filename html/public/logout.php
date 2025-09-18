<?php
/**
 * 로그아웃 처리: Access Token으로 사용자 인증 후 Refresh Token 전체 폐기
 * 요청(JSON, POST):
 * - access_token: string (Authorization 헤더 사용 가능)
 *
 * 응답:
 * - 200 OK: { success: true }
 * - 400/401/405/500: 에러
 */

require_once __DIR__ . '/../shared/Database.php';
require_once __DIR__ . '/../shared/Repositories/RefreshTokenRepository.php';
require_once __DIR__ . '/../shared/Container/Container.php';
require_once __DIR__ . '/../shared/Container/ServiceProvider.php';
require_once __DIR__ . '/../shared/Container/AppProvider.php';
require_once __DIR__ . '/../shared/Container/UtilProvider.php';
require_once __DIR__ . '/../shared/Container/AuthProvider.php';
require_once __DIR__ . '/../shared/Container/ResponseProvider.php';
require_once __DIR__ . '/../shared/Container/RequestProvider.php';
require_once __DIR__ . '/../shared/Container/GuardProvider.php';

$container = new Container();
(new AppProvider())->register($container);
(new UtilProvider())->register($container);
(new AuthProvider())->register($container);
(new ResponseProvider())->register($container);
(new RequestProvider())->register($container);
(new GuardProvider())->register($container);
/** @var ResponseService $response */
$response = $container->get(ResponseService::class);
/** @var RequestService $request */
$request = $container->get(RequestService::class);
$response->setJsonResponseHeader();
$request->assertMethod('POST');
$body = $request->readJsonBody();
/** @var AccessTokenGuardService $guard */
$guard = $container->get(AccessTokenGuardService::class);
$payload = $guard->requirePayload($body);

$userId = isset($payload['sub']) ? (string)$payload['sub'] : '';

$pdo = null;
try {
    $pdo = Database::pdo();
    /** @var RefreshTokenRepository $refreshRepo */
    $refreshRepo = $container->get(RefreshTokenRepository::class);
    $refreshRepo->revokeAllByUserId($userId);

    $response->json(200, array('success' => true));
} catch (PDOException $e) {
    if ($pdo && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    $response->exceptionDbError('데이터베이스 오류가 발생했습니다.', $e);
} catch (Exception $e) {
    if ($pdo && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    $response->exceptionInternal('로그아웃 처리 중 오류가 발생했습니다.', $e);
}
