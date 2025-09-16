<?php
class Http {
    public static function json($statusCode, $payload) {
        http_response_code($statusCode);
        header('Content-Type: application/json; charset=UTF-8');
        echo json_encode($payload);
        exit;
    }

    // 공통: JSON 응답 헤더 설정
    public static function setJsonResponseHeader() {
        header('Content-Type: application/json; charset=UTF-8');
    }

    // 공통: HTTP 메서드 검증(405)
    // $allowed: string 또는 string[]
    public static function assertMethod($allowed) {
        if (!is_array($allowed)) {
            $allowed = array($allowed);
        }
        $method = isset($_SERVER['REQUEST_METHOD']) ? $_SERVER['REQUEST_METHOD'] : '';
        if (!in_array($method, $allowed, true)) {
            header('Allow: ' . implode(', ', $allowed), true, 405);
            self::json(405, array(
                'success' => false,
                'error' => 'METHOD_NOT_ALLOWED',
                'message' => implode(', ', $allowed) . '만 지원합니다.'
            ));
        }
    }

    // 공통: JSON 본문 파싱(400)
    public static function readJsonBody() {
        $raw = file_get_contents('php://input');
        $body = json_decode($raw, true);
        if (!is_array($body)) {
            self::json(400, array(
                'success' => false,
                'error' => 'INVALID_JSON',
                'message' => 'JSON 본문을 파싱할 수 없습니다.'
            ));
        }
        return $body;
    }

    // 간단한 예외 응답 헬퍼들
    public static function exception($status, $error, $message) {
        self::json($status, array(
            'success' => false,
            'error' => $error,
            'message' => $message
        ));
    }

    public static function badRequest($error, $message) {
        self::exception(400, $error, $message);
    }

    public static function exceptionDbError($message = '데이터베이스 오류가 발생했습니다.') {
        self::exception(500, 'DB_ERROR', $message);
    }

    public static function exceptionInternal($message = '내부 오류가 발생했습니다.', $error = 'INTERNAL_ERROR') {
        self::exception(500, $error, $message);
    }
}
