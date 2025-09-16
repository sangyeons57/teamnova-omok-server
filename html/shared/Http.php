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

    // 공통: 예외 가드. 콜백 실행, 예외 매핑, 자동 응답, 트랜잭션 롤백
    // $fn: function($setContext) { $setContext(['pdo'=>$pdo]); return ['status'=>int, 'payload'=>array]; }
    // $options['exceptionMap']: [
    //   'PDOException' => ['status'=>500,'error'=>'DB_ERROR','message'=>'...'],
    //   'DATA_INTEGRITY_ERROR' => ['status'=>500,'error'=>'DATA_INTEGRITY_ERROR','message'=>'...'],
    //   'Exception' => ['status'=>500,'error'=>'INTERNAL_ERROR','message'=>'...'],
    // ]
    public static function guard($fn, $options = array()) {
        $context = array(); // ex: ['pdo' => $pdo]
        $setContext = function ($ctx) use (&$context) {
            if (is_array($ctx)) {
                $context = array_merge($context, $ctx);
            }
        };

        try {
            $result = call_user_func($fn, $setContext);
            if (is_array($result) && isset($result['status']) && isset($result['payload'])) {
                self::json($result['status'], $result['payload']);
            }
            self::json(500, array(
                'success' => false,
                'error' => 'INTERNAL_ERROR',
                'message' => '컨트롤러 반환 형식이 올바르지 않습니다.'
            ));
        } catch (PDOException $e) {
            if (isset($context['pdo']) && $context['pdo'] instanceof PDO) {
                if ($context['pdo']->inTransaction()) {
                    $context['pdo']->rollBack();
                }
            }
            $mapped = self::mapException($e, $options);
            self::json($mapped['status'], array(
                'success' => false,
                'error' => $mapped['error'],
                'message' => $mapped['message']
            ));
        } catch (Exception $e) {
            if (isset($context['pdo']) && $context['pdo'] instanceof PDO) {
                if ($context['pdo']->inTransaction()) {
                    $context['pdo']->rollBack();
                }
            }
            $mapped = self::mapException($e, $options);
            self::json($mapped['status'], array(
                'success' => false,
                'error' => $mapped['error'],
                'message' => $mapped['message']
            ));
        }
    }

    private static function mapException($e, $options) {
        $map = isset($options['exceptionMap']) && is_array($options['exceptionMap']) ? $options['exceptionMap'] : array();
        $cls = get_class($e);
        $msg = $e->getMessage();

        if (isset($map[$cls]) && is_array($map[$cls])) {
            return self::normalizeErrMap($map[$cls], $cls);
        }
        if (isset($map[$msg]) && is_array($map[$msg])) {
            return self::normalizeErrMap($map[$msg], $cls);
        }

        if ($e instanceof PDOException) {
            return array('status' => 500, 'error' => 'DB_ERROR', 'message' => '데이터베이스 오류가 발생했습니다.');
        }
        return array('status' => 500, 'error' => 'INTERNAL_ERROR', 'message' => '내부 오류가 발생했습니다.');
    }

    private static function normalizeErrMap($entry, $fallbackClass) {
        $status = isset($entry['status']) ? intval($entry['status']) : 500;
        $error = isset($entry['error']) ? $entry['error'] : $fallbackClass;
        $message = isset($entry['message']) ? $entry['message'] : '오류가 발생했습니다.';
        return array('status' => $status, 'error' => $error, 'message' => $message);
    }
}
