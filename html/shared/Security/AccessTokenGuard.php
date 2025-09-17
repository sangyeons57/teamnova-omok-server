<?php
require_once __DIR__ . '/../Http.php';
require_once __DIR__ . '/TokenService.php';

/**
 * 액세스 토큰 공통 가드:
 * - Authorization: Bearer ... 또는 body.access_token에서 토큰을 추출
 * - TokenService::validateAccessToken으로 검증(순수 반환)
 * - 검증 실패 시 여기서 401 응답 후 종료
 * - 성공 시 JWT payload 배열 반환
 */
final class AccessTokenGuard
{
    public static function requirePayload(array $body): array
    {
        $accessToken = '';
        $authHeader = isset($_SERVER['HTTP_AUTHORIZATION']) ? trim($_SERVER['HTTP_AUTHORIZATION']) : '';
        if ($authHeader !== '' && stripos($authHeader, 'Bearer ') === 0) {
            $accessToken = trim(substr($authHeader, 7));
        } elseif (isset($body['access_token'])) {
            $accessToken = is_string($body['access_token']) ? trim($body['access_token']) : '';
        }

        $tokenService = new TokenService(null, null);
        $result = $tokenService->validateAccessToken($accessToken);

        if (!isset($result['valid']) || $result['valid'] !== true) {
            $error = isset($result['error']) ? $result['error'] : 'ACCESS_TOKEN_INVALID';
            if ($error === 'ACCESS_TOKEN_EXPIRED') {
                Http::json(401, array(
                    'success' => false,
                    'error' => 'ACCESS_TOKEN_EXPIRED',
                    'message' => '액세스 토큰이 만료되었습니다. refresh_token으로 재발급을 요청하세요.',
                    'retry_with_refresh' => true
                ));
            } else {
                Http::json(401, array(
                    'success' => false,
                    'error' => 'ACCESS_TOKEN_INVALID',
                    'message' => '액세스 토큰이 유효하지 않습니다. 새 로그인 또는 refresh_token 재발급을 시도하세요.',
                    'retry_with_refresh' => true
                ));
            }
        }

        return $result['payload'];
    }
}
