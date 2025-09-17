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
require_once __DIR__ . '/../shared/Http.php';
require_once __DIR__ . '/../shared/Security/TokenService.php';
require_once __DIR__ . '/../shared/Security/AccessTokenGuard.php';
require_once __DIR__ . '/../shared/Repositories/RefreshTokenRepository.php';

Http::setJsonResponseHeader();
Http::assertMethod('POST');
$body = Http::readJsonBody();

$payload = AccessTokenGuard::requirePayload($body);

$userId = isset($payload['sub']) ? (string)$payload['sub'] : '';

$pdo = null;
try {
    $pdo = Database::pdo();
    $refreshRepo = new RefreshTokenRepository($pdo);
    $refreshRepo->revokeAllByUserId($userId);

    Http::json(200, array('success' => true));
} catch (PDOException $e) {
    if ($pdo && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    Http::exceptionDbError('데이터베이스 오류가 발생했습니다.', $e);
} catch (Exception $e) {
    if ($pdo && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    Http::exceptionInternal('로그아웃 처리 중 오류가 발생했습니다.', $e);
}
