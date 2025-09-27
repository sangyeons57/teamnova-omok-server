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
        $accessToken = $this->resolveAccessToken();
        $result = $this->tokenService->validateAccessToken($accessToken);

        if (!$result['valid']) {
            $this->respondTokenError($result);
        }

        return $result['payload'];
    }

    private function resolveAccessToken(): string
    {
        $token = $this->extractBearerTokenFromHeader();
        if ($token !== '') {
            return $token;
        }

        $token = $this->extractAccessTokenFromRequest();
        if ($token !== '') {
            return $token;
        }

        return '';
    }

    private function extractBearerTokenFromHeader(): string
    {
        $header = $this->getAuthorizationHeaderValue();
        if ($header === '') {
            return '';
        }

        if (preg_match('/^Bearer\s+(.+)$/i', $header, $matches) !== 1) {
            return '';
        }

        return trim($matches[1]);
    }

    private function getAuthorizationHeaderValue(): string
    {
        foreach (array('HTTP_AUTHORIZATION', 'REDIRECT_HTTP_AUTHORIZATION', 'AUTHORIZATION') as $key) {
            if (isset($_SERVER[$key])) {
                $value = trim((string) $_SERVER[$key]);
                if ($value !== '') {
                    return $value;
                }
            }
        }

        if (function_exists('getallheaders')) {
            $headers = getallheaders();
            if (is_array($headers)) {
                foreach ($headers as $name => $value) {
                    if (is_string($name) && strcasecmp($name, 'Authorization') === 0 && is_string($value)) {
                        $value = trim($value);
                        if ($value !== '') {
                            return $value;
                        }
                    }
                }
            }
        }

        return '';
    }

    private function extractAccessTokenFromRequest(): string
    {
        if (!isset($_REQUEST['access_token']) || !is_string($_REQUEST['access_token'])) {
            return '';
        }

        return trim($_REQUEST['access_token']);
    }

    private function respondTokenError(array $result): void
    {
        $error = $result['error'] ?? 'ACCESS_TOKEN_INVALID';
        $isExpired = ($error === 'ACCESS_TOKEN_EXPIRED');

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

        $payload = array(
            'success' => false,
            'error' => $error,
            'message' => $isExpired
                ? '액세스 토큰이 만료되었습니다. refresh_token으로 재발급을 요청하세요.'
                : '액세스 토큰이 유효하지 않습니다. 새 로그인 또는 refresh_token 재발급을 시도하세요.',
            'retry_with_refresh' => true,
            'source' => 'TokenService::validateAccessToken',
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
}
