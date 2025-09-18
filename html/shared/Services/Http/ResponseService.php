<?php
class ResponseService
{
    public function json(int $statusCode, array $payload): void
    {
        http_response_code($statusCode);
        header('Content-Type: application/json; charset=UTF-8');
        echo json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        exit;
    }

    public function setJsonResponseHeader(): void
    {
        header('Content-Type: application/json; charset=UTF-8');
    }

    public function exception(int $status, string $error, string $message): void
    {
        $this->json($status, array(
            'success' => false,
            'error' => $error,
            'message' => $message
        ));
    }

    public function badRequest(string $error, string $message): void
    {
        $this->exception(400, $error, $message);
    }

    public function exceptionWithException(int $status, string $error, string $message, Exception $e): void
    {
        $payload = array(
            'success' => false,
            'error' => $error,
            'message' => $message,
            'exception' => array(
                'class' => get_class($e),
                'code'  => $e->getCode(),
                'msg'   => $e->getMessage(),
                'file'  => $e->getFile(),
                'line'  => $e->getLine(),
                'trace' => $this->formatTrace($e->getTrace())
            )
        );
        $prev = $e->getPrevious();
        if ($prev instanceof Exception) {
            $payload['exception']['previous'] = array(
                'class' => get_class($prev),
                'code'  => $prev->getCode(),
                'msg'   => $prev->getMessage(),
            );
        }
        $this->json($status, $payload);
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

    private function formatTrace($trace): array
    {
        $out = array();
        if (!is_array($trace)) {
            return $out;
        }
        $limit = 10;
        $i = 0;
        foreach ($trace as $frame) {
            if ($i++ >= $limit) break;
            $out[] = array(
                'file' => isset($frame['file']) ? $frame['file'] : null,
                'line' => isset($frame['line']) ? $frame['line'] : null,
                'func' => (isset($frame['class']) ? $frame['class'] . $frame['type'] : '') . (isset($frame['function']) ? $frame['function'] : '')
            );
        }
        return $out;
    }
}
