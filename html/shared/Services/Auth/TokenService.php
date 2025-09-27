<?php
require_once __DIR__ . '/../../Config.php';
require_once __DIR__ . '/../../Repositories/UserRepository.php';
require_once __DIR__ . '/JwtService.php';

class TokenService {
    private RefreshTokenRepository $refreshRepo;
    private UserRepository $userRepo ;
    private CryptoService $crypto;
    private ClockService $clock;

    public function __construct($refreshRepo, $userRepo, $crypto, $clock) {
        $this->refreshRepo = $refreshRepo;
        $this->userRepo = $userRepo;
        $this->crypto = $crypto;
        $this->clock = $clock;
    }

    public function issue($userId): array
    {
        $now = time();
        $accessExp = $now + Config::accessTtlSec();

        // 최신 role 조회
        $user = $this->userRepo->findById($userId);
        $role = ($user && isset($user['role'])) ? $user['role'] : 'USER';

        $jwt = JwtService::encodeHS256(array(
            'iss' => 'teamnova-omok',
            'sub' => $userId,
            'iat' => $now,
            'exp' => $accessExp,
            'role' => $role,
            'scope' => 'user',
        ), Config::jwtSecret());

        $refresh = $this->crypto->base64url($this->crypto->randomBytes(32));
        $hash = $this->crypto->sha256($refresh);

        $expiresAt = $this->clock->nowUtc();
        $expiresAt->modify('+' . Config::refreshTtlDays() . ' days');

        $this->refreshRepo->save($userId, $hash, $expiresAt);

        return array(
            'access_token' => $jwt,
            'token_type' => 'Bearer',
            'expires_in' => Config::accessTtlSec(),
            'refresh_token' => $refresh,
        );
    }

    /**
     * 기존 리프레시 토큰을 비활성화(revoked_at 설정)합니다.
     * 존재하지 않거나 이미 비활성화된 경우에도 에러 없이 종료합니다(멱등).
     * @throws Exception
     */
    public function revoke(string $refreshToken): void
    {
        $hash = $this->crypto->sha256($refreshToken);
        if (method_exists($this->refreshRepo, 'revokeByHash')) {
            $this->refreshRepo->revokeByHash($hash);
            return;
        }
        throw new Exception('REFRESH_REPO_REVOKE_NOT_SUPPORTED');
    }

    /**
     * 지정한 사용자의 모든 리프레시 토큰을 비활성화합니다.
     * 이미 비활성화된 토큰은 무시합니다(멱등).
     * @throws Exception
     */
    public function revokeAllByUserId(string $userId): void
    {
        if (method_exists($this->refreshRepo, 'revokeAllByUserId')) {
            $this->refreshRepo->revokeAllByUserId($userId);
            return;
        }
        throw new Exception('REFRESH_REPO_REVOKE_NOT_SUPPORTED');
    }

    /**
     * 리프레시 토큰 검증 → 회전(무효화) → issue 재사용하여 새 토큰들 발급.
     *
     * @param string $refreshToken 클라이언트가 보낸 기존 리프레시 토큰(원본)
     * @return array { access_token, token_type, expires_in, refresh_token }
     * @throws Exception INVALID_REFRESH_TOKEN | REFRESH_TOKEN_EXPIRED | REFRESH_TOKEN_REVOKED
     */
    public function refresh(string $refreshToken): array
    {
        $hash = $this->crypto->sha256($refreshToken);

        if (!method_exists($this->refreshRepo, 'findByHash')) {
            throw new Exception('REFRESH_REPO_FIND_NOT_SUPPORTED');
        }

        $record = $this->refreshRepo->findByHash($hash);
        if (!$record) {
            throw new Exception('INVALID_REFRESH_TOKEN');
        }

        $nowUtc = $this->clock->nowUtc();

        // revoked_at 검사
        if (isset($record['revoked_at']) && $record['revoked_at'] !== '') {
            throw new Exception('REFRESH_TOKEN_REVOKED');
        }

        // expires_at 검사
        $expiresAt = new DateTime($record['expires_at'], new DateTimeZone('UTC'));
        if ($expiresAt <= $nowUtc) {
            // 방어적으로 revoke 시도 (멱등)
            if (method_exists($this->refreshRepo, 'revokeByHash')) {
                $this->refreshRepo->revokeByHash($hash);
            }
            throw new Exception('REFRESH_TOKEN_EXPIRED');
        }

        $userId = $record['user_id'];

        // 회전: 기존 RT 즉시 비활성화
        if (method_exists($this->refreshRepo, 'revokeByHash')) {
            $this->refreshRepo->revokeByHash($hash);
        } else {
            throw new Exception('REFRESH_REPO_REVOKE_NOT_SUPPORTED');
        }

        // 새 토큰 발급(AT/RT) - role은 issue 내부에서 UserRepository로 조회
        return $this->issue($userId);
    }

    private static function b64urlDecode(string $data): false|string
    {
        $data = strtr($data, '-_', '+/');
        $pad = strlen($data) % 4;
        if ($pad) {
            $data .= str_repeat('=', 4 - $pad);
        }
        $decoded = base64_decode($data, true);
        return ($decoded === false) ? false : $decoded;
    }

    private function makeInvalidResult(string $stage, string $message, string $error = 'ACCESS_TOKEN_INVALID', array $extra = array()): array
    {
        $result = array(
            'valid' => false,
            'error' => $error,
            'stage' => $stage,
            'message' => $message
        );
        foreach ($extra as $key => $value) {
            if ($value !== null) {
                $result[$key] = $value;
            }
        }
        return $result;
    }

    /**
     * 액세스 토큰(JWT, HS256) 유효성 검증(순수).
     * - 성공: array('valid'=>true, 'payload'=>array)
     * - 실패: array('valid'=>false, 'error'=>'ACCESS_TOKEN_INVALID'|'ACCESS_TOKEN_EXPIRED', 'message'=>string, 'stage'=>string, ...)
     */
    public function validateAccessToken(string $jwt): array
    {
        if ($jwt === '') {
            return $this->makeInvalidResult('missing_token', 'Authorization 헤더에서 Bearer 토큰을 찾을 수 없습니다.');
        }

        $parts = explode('.', $jwt);
        if (count($parts) !== 3) {
            return $this->makeInvalidResult('malformed_jwt', 'JWT 형식이 잘못되었습니다. 3개의 파트(header.payload.signature)가 필요합니다.');
        }
        list($h, $p, $s) = $parts;

        $decoded = array();
        foreach (array('header' => $h, 'payload' => $p, 'signature' => $s) as $partName => $encoded) {
            $value = self::b64urlDecode($encoded);
            if ($value === false) {
                return $this->makeInvalidResult('b64_decode_' . $partName, 'JWT ' . $partName . ' 부분을 base64url로 디코딩할 수 없습니다.');
            }
            $decoded[$partName] = $value;
        }

        $header = json_decode($decoded['header'], true);
        if (!is_array($header)) {
            return $this->makeInvalidResult('json_decode_header', 'JWT header JSON 파싱에 실패했습니다.');
        }

        $payload = json_decode($decoded['payload'], true);
        if (!is_array($payload)) {
            return $this->makeInvalidResult('json_decode_payload', 'JWT payload JSON 파싱에 실패했습니다.');
        }

        if (!isset($header['alg']) || $header['alg'] !== 'HS256') {
            return $this->makeInvalidResult('unsupported_alg', '지원하지 않는 JWT 서명 알고리즘입니다.', 'ACCESS_TOKEN_INVALID', array('alg' => $header['alg'] ?? null));
        }

        // 서명 검증
        $signingInput = $h . '.' . $p;
        $expectedSig = hash_hmac('sha256', $signingInput, Config::jwtSecret(), true);
        if (!hash_equals($expectedSig, $decoded['signature'])) {
            return $this->makeInvalidResult('signature_mismatch', 'JWT 서명 검증에 실패했습니다.');
        }

        $now = time();

        if (!isset($payload['exp']) || !is_numeric($payload['exp'])) {
            return $this->makeInvalidResult('missing_exp', 'JWT payload에 만료 시각(exp) 클레임이 없습니다.');
        }

        $exp = (int) $payload['exp'];
        if ($exp <= $now) {
            return $this->makeInvalidResult('expired', '액세스 토큰이 만료되었습니다.', 'ACCESS_TOKEN_EXPIRED', array('expired_at' => $exp));
        }

        if (isset($payload['iss']) && $payload['iss'] !== 'teamnova-omok') {
            return $this->makeInvalidResult('invalid_iss', 'JWT 발급자(iss) 값이 예상과 다릅니다.', 'ACCESS_TOKEN_INVALID', array('iss' => $payload['iss']));
        }

        return array('valid' => true, 'payload' => $payload);
    }
}

