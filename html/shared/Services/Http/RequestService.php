<?php
require_once __DIR__ . '/ResponseService.php';

class RequestService
{
    private $response;

    public function __construct($response)
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
                'message' => implode(', ', $allowed) . '만 지원합니다.'
            ));
        }
    }

    public function readJsonBody(): array
    {
        $raw = file_get_contents('php://input');
        $body = json_decode($raw, true);
        if (!is_array($body)) {
            $this->response->json(400, array(
                'success' => false,
                'error' => 'INVALID_JSON',
                'message' => 'JSON 본문을 파싱할 수 없습니다.'
            ));
        }
        return $body;
    }
}
