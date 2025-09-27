<?php
require_once __DIR__ . '/../Auth/TokenService.php';
require_once __DIR__ . '/../Http/ResponseService.php';

/**
 * 액세스 토큰 가드 서비스
 * - Authorization 헤더에서 Bearer 토큰을 추출
 * - TokenService::validateAccessToken으로 검증
 * - 실패 시 401 응답 반환 후 종료
 * - 성공 시 JWT payload 배열 반환
 */
class AccessTokenGuardService
{
    private TokenService $tokenService;
    private ResponseService $response;

    public function __construct(TokenService $tokenService, ResponseService $response)
    {
        $this->tokenService = $tokenService;
        $this->response = $response;
    }

    public function requirePayload(): array
    {
        $accessToken = '';
        $authHeader = isset($_SERVER['HTTP_AUTHORIZATION']) ? trim($_SERVER['HTTP_AUTHORIZATION']) : '';
        if ($authHeader !== '' && stripos($authHeader, 'Bearer ') === 0) {
            $accessToken = trim(substr($authHeader, 7));
        }

        $result = $this->tokenService->validateAccessToken($accessToken);

        if (!$result['valid']) {
            $error = $result['error'] ?? 'ACCESS_TOKEN_INVALID';
            $detail = '';
            if (isset($result['message']) && is_string($result['message'])) {
                $detail = trim($result['message']);
            }
            $stage = null;
            if (isset($result['stage']) && is_string($result['stage'])) {
                $stageValue = trim($result['stage']);
                if ($stageValue !== '') {
                    $stage = $stageValue;
                }
            }

            $isExpired = ($error === 'ACCESS_TOKEN_EXPIRED');
            $defaultMessage = $isExpired
                ? '액세스 토큰이 만료되었습니다. refresh_token으로 재발급을 요청하세요.'
                : '액세스 토큰이 유효하지 않습니다. 새 로그인 또는 refresh_token 재발급을 시도하세요.';

            $payload = array(
                'success' => false,
                'error' => $error,
                'message' => $defaultMessage,
                'retry_with_refresh' => true,
                'source' => 'TokenService::validateAccessToken'
            );

            if ($detail !== '') {
                $payload['reason'] = $detail;
            }
            if ($stage !== null) {
                $payload['stage'] = $stage;
            }

            foreach (array('expired_at', 'alg', 'iss') as $key) {
                if (isset($result[$key])) {
                    $payload[$key] = $result[$key];
                }
            }

            $bearerDescription = $detail !== ''
                ? $detail
                : ($isExpired ? 'Access token expired' : 'Signature verification failed');

            $this->response->bearerUnauthorized(
                $payload,
                'invalid_token',
                $bearerDescription,
                'api'
            );
        }

        return $result['payload'];
    }
}
