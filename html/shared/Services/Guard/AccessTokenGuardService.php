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
        $tokenSource = 'none';

        $headerInfo = $this->resolveAuthorizationHeader();
        if ($headerInfo['value'] !== '') {
            if (preg_match('/^Bearer\s+(.+)$/i', $headerInfo['value'], $matches) === 1) {
                $accessToken = trim($matches[1]);
                $tokenSource = 'authorization_header';
            } else {
                $tokenSource = 'authorization_header_unparsable';
            }
        }

        if ($accessToken === '' && isset($_REQUEST['access_token']) && is_string($_REQUEST['access_token'])) {
            $candidate = trim($_REQUEST['access_token']);
            if ($candidate !== '') {
                $accessToken = $candidate;
                $tokenSource = 'request_access_token_field';
            }
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

            if ($tokenSource !== 'none') {
                $payload['token_source'] = $tokenSource;
            }
            if ($headerInfo['source'] !== null) {
                $payload['header_source'] = $headerInfo['source'];
            }
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

    private function resolveAuthorizationHeader(): array
    {
        $candidates = array();

        foreach (array('HTTP_AUTHORIZATION', 'REDIRECT_HTTP_AUTHORIZATION', 'AUTHORIZATION') as $serverKey) {
            if (isset($_SERVER[$serverKey]) && is_string($_SERVER[$serverKey])) {
                $candidates[] = array(
                    'value' => $_SERVER[$serverKey],
                    'source' => 'server:' . $serverKey
                );
            }
        }

        if (function_exists('getallheaders')) {
            $headers = getallheaders();
            if (is_array($headers)) {
                foreach ($headers as $name => $value) {
                    if (is_string($name) && strcasecmp($name, 'Authorization') === 0 && is_string($value)) {
                        $candidates[] = array(
                            'value' => $value,
                            'source' => 'getallheaders:' . $name
                        );
                    }
                }
            }
        }

        foreach ($candidates as $candidate) {
            $value = trim($candidate['value']);
            if ($value !== '') {
                return array(
                    'value' => $value,
                    'source' => $candidate['source']
                );
            }
        }

        return array(
            'value' => '',
            'source' => null
        );
    }
}
