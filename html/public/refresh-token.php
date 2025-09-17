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
require_once __DIR__ . '/../shared/Http.php';
require_once __DIR__ . '/../shared/Validation.php';
require_once __DIR__ . '/../shared/Util/Clock.php';
require_once __DIR__ . '/../shared/Security/TokenService.php';
require_once __DIR__ . '/../shared/Repositories/UserRepository.php';
require_once __DIR__ . '/../shared/Repositories/RefreshTokenRepository.php';

Http::setJsonResponseHeader();
Http::assertMethod('POST');
$body = Http::readJsonBody();

$refreshToken = isset($body['refresh_token']) ? trim($body['refresh_token']) : '';
if ($refreshToken === '') {
    Http::json(400, array(
        'success' => false,
        'error' => 'INVALID_REFRESH_TOKEN',
        'message' => 'refresh_token이 비어있습니다.'
    ));
}

$pdo = null;
try {
    $pdo = Database::pdo();

    $refreshRepo = new RefreshTokenRepository($pdo);
    $userRepo = new UserRepository($pdo);
    $tokenService = new TokenService($refreshRepo, $userRepo);

    $tokens = $tokenService->refresh($refreshToken);

    Http::json(200, array_merge(array('success' => true), $tokens));

} catch (Exception $e) {
    $code = $e->getMessage();

    if ($code === 'INVALID_REFRESH_TOKEN') {
        Http::json(401, array(
            'success' => false,
            'error' => 'INVALID_REFRESH_TOKEN',
            'message' => '리프레시 토큰이 유효하지 않습니다. 다시 로그인해 주세요.'
        ));
    } else if ($code === 'REFRESH_TOKEN_EXPIRED') {
        Http::json(401, array(
            'success' => false,
            'error' => 'REFRESH_TOKEN_EXPIRED',
            'message' => '리프레시 토큰이 만료되었습니다. 다시 로그인해 주세요.'
        ));
    } else if ($code === 'REFRESH_TOKEN_REVOKED') {
        Http::json(401, array(
            'success' => false,
            'error' => 'REFRESH_TOKEN_REVOKED',
            'message' => '이미 사용되었거나 취소된 리프레시 토큰입니다. 다시 로그인해 주세요.'
        ));
    } else {
        Http::exceptionInternal('내부 오류가 발생했습니다.', $e);
    }
}
