<?php
/**
 * 토큰 재발급(Refresh) 엔드포인트
 * 입력(JSON, POST):
 * - refresh_token: string (필수)
 *
 * 응답:
 * - 200 OK: { success: true, access_token, refresh_token, token_type, expires_in }
 * - 401 Unauthorized:
 *    - INVALID_REFRESH_TOKEN | REFRESH_TOKEN_EXPIRED | REFRESH_TOKEN_REVOKED
 * - 400/405/500: 에러
 */
require_once __DIR__ . '/../shared/Config.php';
require_once __DIR__ . '/../shared/Database.php';
require_once __DIR__ . '/../shared/Services/Auth/TokenService.php';
require_once __DIR__ . '/../shared/Repositories/UserRepository.php';
require_once __DIR__ . '/../shared/Repositories/RefreshTokenRepository.php';
require_once __DIR__ . '/../shared/Container/Container.php';
require_once __DIR__ . '/../shared/Container/ServiceProvider.php';
require_once __DIR__ . '/../shared/Container/AppProvider.php';
require_once __DIR__ . '/../shared/Container/UtilProvider.php';
require_once __DIR__ . '/../shared/Container/AuthProvider.php';
require_once __DIR__ . '/../shared/Container/ResponseProvider.php';
require_once __DIR__ . '/../shared/Container/RequestProvider.php';

$container = new Container();
(new AppProvider())->register($container);
(new UtilProvider())->register($container);
(new AuthProvider())->register($container);
(new ResponseProvider())->register($container);
(new RequestProvider())->register($container);
/** @var ResponseService $response */
$response = $container->get(ResponseService::class);
/** @var RequestService $request */
$request = $container->get(RequestService::class);
$response->setJsonResponseHeader();
$request->assertMethod('POST');
$body = $request->readJsonBody();

$refreshToken = isset($body['refresh_token']) ? trim($body['refresh_token']) : '';
if ($refreshToken === '') {
    $response->json(400, array(
        'success' => false,
        'error' => 'INVALID_REFRESH_TOKEN',
        'message' => 'refresh_token이 비어있습니다.'
    ));
}

$pdo = null;
try {
    $pdo = Database::pdo();

    /** @var TokenService $tokenService */
    $tokenService = $container->get(TokenService::class);
    $tokens = $tokenService->refresh($refreshToken);

    $response->json(200, array_merge(array('success' => true), $tokens));

} catch (Exception $e) {
    $code = $e->getMessage();

    if ($code === 'INVALID_REFRESH_TOKEN') {
        $response->json(401, array(
            'success' => false,
            'error' => 'INVALID_REFRESH_TOKEN',
            'message' => '리프레시 토큰이 유효하지 않습니다. 다시 로그인해 주세요.'
        ));
    } else if ($code === 'REFRESH_TOKEN_EXPIRED') {
        $response->json(401, array(
            'success' => false,
            'error' => 'REFRESH_TOKEN_EXPIRED',
            'message' => '리프레시 토큰이 만료되었습니다. 다시 로그인해 주세요.'
        ));
    } else if ($code === 'REFRESH_TOKEN_REVOKED') {
        $response->json(401, array(
            'success' => false,
            'error' => 'REFRESH_TOKEN_REVOKED',
            'message' => '이미 사용되었거나 취소된 리프레시 토큰입니다. 다시 로그인해 주세요.'
        ));
    } else {
        $response->exceptionInternal('내부 오류가 발생했습니다.', $e);
    }
}
