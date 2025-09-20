<?php
require_once __DIR__ . '/ResponseService.php';

class RequestService
{
    private ResponseService $response;

    public function __construct(ResponseService $response)
    {
        $this->response = $response;
    }

    // $allowed: string|string[]
    public function assertMethod($allowed): void
    {
        if (!is_array($allowed)) {
            $allowed = array($allowed);
        }
        $method = isset($_SERVER['REQUEST_METHOD']) ? $_SERVER['REQUEST_METHOD'] : '';
        if (!in_array($method, $allowed, true)) {
            header('Allow: ' . implode(', ', $allowed), true, 405);
            $this->response->json(405, array(
                'success' => false,
                'error' => 'METHOD_NOT_ALLOWED',
                'message' => implode(', ', $allowed) . '만 허용합니다.'
            ));
        }
    }

    public function readJsonBody(): array
    {
        $raw = file_get_contents('php://input');
        $body = json_decode($raw, true);
        if (!is_array($body)) {
            $this->response->error('INVALID_JSON', 400, 'JSON 본문을 해석할 수 없습니다.');
        }
        return $body;
    }

    public function readEnvelope(?string $assertMethod = null): array
    {
        if ($assertMethod !== null) {
            $this->assertMethod($assertMethod);
        }

        $envelope = $this->readJsonBody();
        $requestId = $this->extractStringField($envelope, 'requestId');
        $timestamp = $this->extractStringField($envelope, 'timestamp');
        $path = $this->extractStringField($envelope, 'path');

        if (!array_key_exists('body', $envelope)) {
            $this->response->error('INVALID_REQUEST_ENVELOPE', 400, 'body 필드가 누락되었습니다.');
        }

        $body = $envelope['body'];
        if (!is_array($body)) {
            $this->response->error('INVALID_BODY_FORMAT', 400, 'body 필드는 객체여야 합니다.');
        }

        $this->response->overrideRequestId($requestId);

        return array(
            'requestId' => $requestId,
            'timestamp' => $timestamp,
            'path' => $path,
            'body' => $body,
        );
    }

    public function readBody(?string $assertMethod = null): array
    {
        $envelope = $this->readEnvelope($assertMethod);
        return $envelope['body'];
    }

    private function extractStringField(array $envelope, string $key): string
    {
        if (!array_key_exists($key, $envelope)) {
            $this->response->error('INVALID_REQUEST_ENVELOPE', 400, $key . ' 필드가 누락되었습니다.');
        }

        $value = $envelope[$key];
        if (!is_string($value)) {
            $this->response->error('INVALID_REQUEST_ENVELOPE', 400, $key . ' 필드는 문자열이어야 합니다.');
        }

        $value = trim($value);
        if ($value === '') {
            $this->response->error('INVALID_REQUEST_ENVELOPE', 400, $key . ' 필드를 확인할 수 없습니다.');
        }

        return $value;
    }
}
