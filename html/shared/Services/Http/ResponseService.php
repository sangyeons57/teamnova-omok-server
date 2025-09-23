<?php
class ResponseService
{
    private string $apiVersion = '1.0';
    private ?string $requestId = null;
    private ?string $traceId = null;
    private StopwatchService $stopwatch;

    public function __construct(StopwatchService $stopwatch)
    {
        $this->stopwatch = $stopwatch;
        $this->requestId = $this->readOrGenerateId('HTTP_X_REQUEST_ID');
        $this->traceId   = $this->readOrGenerateId('HTTP_X_TRACE_ID');
    }

    public function json(int $statusCode, array $payload): void
    {
        // 이제는 Envelope 없이, 전달받은 payload를 그대로 바디로 반환합니다.
        $this->sendBody($statusCode, $this->ensureObject($payload));
    }

    public function setJsonResponseHeader(): void
    {
        header('Content-Type: application/json; charset=UTF-8');
    }

    public function overrideRequestId(?string $requestId): void
    {
        // 더 이상 본문에서 requestId를 받아 덮어쓰지 않지만, 하위호환을 위해 남겨둡니다.
        if ($requestId === null) {
            return;
        }
        $requestId = trim($requestId);
        if ($requestId === '') {
            return;
        }
        $this->requestId = $requestId;
    }

    // 명시적 성공 응답: 이제 payload만 그대로 출력
    public function success(int $httpStatus, string $type, ?string $id, $payload = null, array $metaOverrides = []): void
    {
        $body = is_array($payload) ? $payload : array();
        $this->sendBody($httpStatus, $this->ensureObject($body));
    }

    // 명시적 오류 응답: 오류 객체를 그대로 출력
    public function error(string $code, int $httpStatus, string $message, ?string $detail = null, ?array $fields = null, array $metaOverrides = []): void
    {
        $this->sendBody($httpStatus, $this->makeError($code, $httpStatus, $message, $detail, $fields));
    }

    public function bearerUnauthorized(
        array $payload,
        string $bearerError = 'invalid_token',
        string $bearerErrorDescription = '',
        string $realm = 'api',
        ?string $scope = null
    ): void {
        $this->setBearerAuthenticateHeader($realm, $bearerError, $bearerErrorDescription, $scope);
        $this->json(401, $payload);
    }

    public function exception(int $status, string $error, string $message): void
    {
        $this->sendBody($status, $this->makeError($error, $status, $message, null, null));
    }

    public function badRequest(string $error, string $message): void
    {
        $this->exception(400, $error, $message);
    }

    public function exceptionWithException(int $status, string $error, string $message, Exception $e): void
    {
        $detail = $e->getMessage();
        $this->sendBody($status, $this->makeError($error, $status, $message, $detail, null));
    }

    public function exceptionDbError(string $message = '데이터베이스 오류가 발생했습니다.', Exception $e = null): void
    {
        if ($e instanceof Exception) {
            $this->exceptionWithException(500, 'DB_ERROR', $message, $e);
            return;
        }
        $this->exception(500, 'DB_ERROR', $message);
    }

    public function exceptionInternal(string $message = '내부 오류가 발생했습니다.', Exception $e = null, string $error = 'INTERNAL_ERROR'): void
    {
        if ($e instanceof Exception) {
            $this->exceptionWithException(500, $error, $message, $e);
            return;
        }
        $this->exception(500, $error, $message);
    }

    private function sendBody(int $httpStatus, array $body): void
    {
        http_response_code($httpStatus);
        header('Content-Type: application/json; charset=UTF-8');

        if ($this->requestId) {
            header('X-Request-Id: ' . $this->requestId);
        }
        if ($this->traceId) {
            header('X-Trace-Id: ' . $this->traceId);
        }

        echo json_encode($this->ensureObject($body), JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        exit;
    }

    private function makeError(string $code, int $httpStatus, string $message, ?string $detail, ?array $fields): array
    {
        $err = array(
            'code'       => $code,
            'httpStatus' => $httpStatus,
            'message'    => $message
        );
        if ($detail !== null) {
            $err['detail'] = $detail;
        }
        if ($fields !== null) {
            $err['fields'] = $fields;
        }
        return $err;
    }

    private function setBearerAuthenticateHeader(
        string $realm,
        ?string $error,
        ?string $errorDescription,
        ?string $scope
    ): void {
        $parts = array('Bearer realm="' . $this->escapeDoubleQuotes($realm) . '"');

        if ($error !== null && $error !== '') {
            $parts[] = 'error="' . $this->escapeDoubleQuotes($error) . '"';
        }

        if ($errorDescription !== null && $errorDescription !== '') {
            $parts[] = 'error_description="' . $this->escapeDoubleQuotes($errorDescription) . '"';
        }

        if ($scope !== null && $scope !== '') {
            $parts[] = 'scope="' . $this->escapeDoubleQuotes($scope) . '"';
        }

        header('WWW-Authenticate: ' . implode(', ', $parts));
    }

    private function escapeDoubleQuotes(string $value): string
    {
        return str_replace('"', '\"', $value);
    }

    private function ensureObject(array $body): array
    {
        // JSON 객체(Map) 형태 보장을 위해 배열을 그대로 반환합니다.
        // PHP에서 연관 배열은 JSON 객체로 직렬화됩니다.
        return $body;
    }

    private function readOrGenerateId(string $serverKey): string
    {
        $val = isset($_SERVER[$serverKey]) ? trim((string)$_SERVER[$serverKey]) : '';
        if ($val !== '') {
            return $val;
        }
        return bin2hex(random_bytes(8));
    }

}
