<?php
require_once __DIR__ . '/../Auth/TokenService.php';
require_once __DIR__ . '/../Http/ResponseService.php';

/**
 * 액세스 토큰 가드 서비스
 * - Authorization 헤더 또는 body.access_token에서 토큰을 추출
 * - TokenService::validateAccessToken으로 검증
 * - 실패 시 401 응답 반환 후 종료
 * - 성공 시 JWT payload 배열 반환
 */
class AccessTokenGuardService
{
    private $tokenService;
    private $response;

    public function __construct($tokenService, $response)
    {
        $this->tokenService = $tokenService;
        $this->response = $response;
    }

    public function requirePayload(array $body): array
    {
        $accessToken = '';
        $authHeader = isset($_SERVER['HTTP_AUTHORIZATION']) ? trim($_SERVER['HTTP_AUTHORIZATION']) : '';
        if ($authHeader !== '' && stripos($authHeader, 'Bearer ') === 0) {
            $accessToken = trim(substr($authHeader, 7));
        } elseif (isset($body['access_token'])) {
            $accessToken = is_string($body['access_token']) ? trim($body['access_token']) : '';
        }

        $result = $this->tokenService->validateAccessToken($accessToken);

        if (!$result['valid']) {
            $error = $result['error'] ?? 'ACCESS_TOKEN_INVALID';
            if ($error === 'ACCESS_TOKEN_EXPIRED') {
                $this->response->json(401, array(
                    'success' => false,
                    'error' => 'ACCESS_TOKEN_EXPIRED',
                    'message' => '액세스 토큰이 만료되었습니다. refresh_token으로 재발급을 요청하세요.',
                    'retry_with_refresh' => true
                ));
            } else {
                $this->response->json(401, array(
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
