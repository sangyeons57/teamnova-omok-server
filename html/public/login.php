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

require_once __DIR__ . '/../shared/Http.php';
require_once __DIR__ . '/../shared/Security/TokenService.php';
require_once __DIR__ . '/../shared/Security/AccessTokenGuard.php';

Http::setJsonResponseHeader();
Http::assertMethod('POST');
$body = Http::readJsonBody();

$payload = AccessTokenGuard::requirePayload($body);

$userId = isset($payload['sub']) ? (string)$payload['sub'] : '';

$response = array(
    'success' => true,
    'user_id' => $userId,
);

if (isset($payload['role'])) {
    $response['role'] = $payload['role'];
}

if (isset($payload['exp']) && is_numeric($payload['exp'])) {
    $response['expires_at'] = (int)$payload['exp'];
}

Http::json(200, $response);
