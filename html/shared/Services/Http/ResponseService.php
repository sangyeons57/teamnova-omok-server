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
        // 하위호환: 기존 payload를 표준 Envelope로 매핑
        $metaOverrides = [];
        if (isset($payload['_meta']) && is_array($payload['_meta'])) {
            $metaOverrides = $payload['_meta'];
            unset($payload['_meta']);
        }

        if (isset($payload['success'])) {
            if ($payload['success'] === true) {
                $this->sendEnvelope(
                    $statusCode,
                    $this->makeData(null, null, $payload),
                    null,
                    $metaOverrides
                );
                return;
            }
            if ($payload['success'] === false) {
                $code    = isset($payload['error']) ? (string)$payload['error'] : 'ERROR';
                $message = isset($payload['message']) ? (string)$payload['message'] : '';
                $detail  = isset($payload['detail']) ? (string)$payload['detail'] : null;
                $fields  = isset($payload['fields']) && is_array($payload['fields']) ? $payload['fields'] : null;

                // 알려진 키를 제외한 나머지는 extra로 보존하여 하위호환성 강화
                $known = array('success' => 1, 'error' => 1, 'message' => 1, 'detail' => 1, 'fields' => 1, '_meta' => 1);
                $extra = array();
                foreach ($payload as $k => $v) {
                    if (!isset($known[$k])) {
                        $extra[$k] = $v;
                    }
                }

                $errorArr = $this->makeError($code, $statusCode, $message, $detail, $fields);
                if (!empty($extra)) {
                    $errorArr['extra'] = $extra;
                }

                $this->sendEnvelope(
                    $statusCode,
                    null,
                    $errorArr,
                    $metaOverrides
                );
                return;
            }
        }

        // success 키가 없으면 성공으로 간주하여 data에 그대로 담는다.
        $this->sendEnvelope(
            $statusCode,
            $this->makeData(null, null, $payload),
            null,
            $metaOverrides
        );
    }

    public function setJsonResponseHeader(): void
    {
        header('Content-Type: application/json; charset=UTF-8');
    }

    // 신규: 명시적 성공 응답
    public function success(int $httpStatus, string $type, ?string $id, $payload = null, array $metaOverrides = []): void
    {
        $this->sendEnvelope($httpStatus, $this->makeData($type, $id, $payload), null, $metaOverrides);
    }

    // 신규: 명시적 오류 응답
    public function error(string $code, int $httpStatus, string $message, ?string $detail = null, ?array $fields = null, array $metaOverrides = []): void
    {
        $this->sendEnvelope($httpStatus, null, $this->makeError($code, $httpStatus, $message, $detail, $fields), $metaOverrides);
    }

    public function exception(int $status, string $error, string $message): void
    {
        $this->sendEnvelope($status, null, $this->makeError($error, $status, $message, null, null), []);
    }

    public function badRequest(string $error, string $message): void
    {
        $this->exception(400, $error, $message);
    }

    public function exceptionWithException(int $status, string $error, string $message, Exception $e): void
    {
        $detail = $e->getMessage();
        $this->sendEnvelope($status, null, $this->makeError($error, $status, $message, $detail, null), []);
    }

    public function exceptionDbError(string $message = '데이터베이스 오류가 발생했습니다.', Exception $e = null): void
    {
        if ($e instanceof Exception) {
            $this->exceptionWithException(500, 'DB_ERROR', $message, $e);
        }
        $this->exception(500, 'DB_ERROR', $message);
    }

    public function exceptionInternal(string $message = '내부 오류가 발생했습니다.', Exception $e = null, string $error = 'INTERNAL_ERROR'): void
    {
        if ($e instanceof Exception) {
            $this->exceptionWithException(500, $error, $message, $e);
        }
        $this->exception(500, $error, $message);
    }

    private function sendEnvelope(int $httpStatus, ?array $data, ?array $error, array $metaOverrides): void
    {
        http_response_code($httpStatus);
        header('Content-Type: application/json; charset=UTF-8');

        if ($this->requestId) {
            header('X-Request-Id: ' . $this->requestId);
        }
        if ($this->traceId) {
            header('X-Trace-Id: ' . $this->traceId);
        }

        $meta = $this->buildMeta($metaOverrides);

        $envelope = array(
            'meta'  => $meta,
            'data'  => $data,
            'error' => $error
        );

        echo json_encode($envelope, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        exit;
    }

    private function buildMeta(array $overrides): array
    {
        $meta = array(
            'apiVersion' => $this->apiVersion,
            'requestId'  => $this->requestId,
            'traceId'    => $this->traceId,
            'elapsedMs'  => $this->stopwatch->elapsedMs(),
        );

        foreach ($overrides as $k => $v) {
            $meta[$k] = $v;
        }
        return $meta;
    }

    private function makeData(?string $type, ?string $id, $payload): array
    {
        return array(
            'type'    => $type,
            'id'      => $id,
            'payload' => $payload
        );
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

    private function readOrGenerateId(string $serverKey): string
    {
        $val = isset($_SERVER[$serverKey]) ? trim((string)$_SERVER[$serverKey]) : '';
        if ($val !== '') {
            return $val;
        }
        return bin2hex(random_bytes(8));
    }

}
