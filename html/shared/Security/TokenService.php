<?php
require_once __DIR__ . '/../Config.php';
require_once __DIR__ . '/../Util/Crypto.php';
require_once __DIR__ . '/../Util/Clock.php';
require_once __DIR__ . '/../Repositories/UserRepository.php';
require_once __DIR__ . '/Jwt.php';

class TokenService {
    private $refreshRepo;
    private $userRepo;

    public function __construct($refreshRepo, $userRepo) {
        $this->refreshRepo = $refreshRepo;
        $this->userRepo = $userRepo;
    }

    public function issue($userId): array
    {
        $now = time();
        $accessExp = $now + Config::accessTtlSec();

        // 최신 role 조회
        $user = $this->userRepo->findById($userId);
        $role = ($user && isset($user['role'])) ? $user['role'] : 'USER';

        $jwt = Jwt::encodeHS256(array(
            'iss' => 'teamnova-omok',
            'sub' => $userId,
            'iat' => $now,
            'exp' => $accessExp,
            'role' => $role,
            'scope' => 'user',
        ), Config::jwtSecret());

        $refresh = Crypto::base64url(Crypto::randomBytes(32));
        $hash = Crypto::sha256($refresh);

        $expiresAt = Clock::nowUtc();
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
     */
    public function revoke(string $refreshToken): void
    {
        $hash = Crypto::sha256($refreshToken);
        if (method_exists($this->refreshRepo, 'revokeByHash')) {
            $this->refreshRepo->revokeByHash($hash);
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
        $hash = Crypto::sha256($refreshToken);

        if (!method_exists($this->refreshRepo, 'findByHash')) {
            throw new Exception('REFRESH_REPO_FIND_NOT_SUPPORTED');
        }

        $record = $this->refreshRepo->findByHash($hash);
        if (!$record) {
            throw new Exception('INVALID_REFRESH_TOKEN');
        }

        $nowUtc = Clock::nowUtc();

        // revoked_at 검사
        if (isset($record['revoked_at']) && $record['revoked_at'] !== null && $record['revoked_at'] !== '') {
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

    private static function b64urlDecode(string $data)
    {
        $data = strtr($data, '-_', '+/');
        $pad = strlen($data) % 4;
        if ($pad) {
            $data .= str_repeat('=', 4 - $pad);
        }
        $decoded = base64_decode($data, true);
        return ($decoded === false) ? false : $decoded;
    }

    /**
     * 액세스 토큰(JWT, HS256) 유효성 검증(순수).
     * - 성공: array('valid'=>true, 'payload'=>array)
     * - 실패: array('valid'=>false, 'error'=>'ACCESS_TOKEN_INVALID'|'ACCESS_TOKEN_EXPIRED')
     */
    public function validateAccessToken(string $jwt): array
    {
        if (!is_string($jwt) || $jwt === '') {
            return array('valid' => false, 'error' => 'ACCESS_TOKEN_INVALID');
        }

        $parts = explode('.', $jwt);
        if (count($parts) !== 3) {
            return array('valid' => false, 'error' => 'ACCESS_TOKEN_INVALID');
        }
        list($h, $p, $s) = $parts;

        $headerJson  = self::b64urlDecode($h);
        $payloadJson = self::b64urlDecode($p);
        $sigBin      = self::b64urlDecode($s);

        if ($headerJson === false || $payloadJson === false || $sigBin === false) {
            return array('valid' => false, 'error' => 'ACCESS_TOKEN_INVALID');
        }

        $header = json_decode($headerJson, true);
        $payload = json_decode($payloadJson, true);
        if (!is_array($header) || !is_array($payload)) {
            return array('valid' => false, 'error' => 'ACCESS_TOKEN_INVALID');
        }

        if (!isset($header['alg']) || $header['alg'] !== 'HS256') {
            return array('valid' => false, 'error' => 'ACCESS_TOKEN_INVALID');
        }

        // 서명 검증
        $signingInput = $h . '.' . $p;
        $expectedSig = hash_hmac('sha256', $signingInput, Config::jwtSecret(), true);
        if (!hash_equals($expectedSig, $sigBin)) {
            return array('valid' => false, 'error' => 'ACCESS_TOKEN_INVALID');
        }

        $now = time();

        // 만료 검사
        if (!isset($payload['exp']) || !is_numeric($payload['exp'])) {
            return array('valid' => false, 'error' => 'ACCESS_TOKEN_INVALID');
        }
        if ((int)$payload['exp'] <= $now) {
            return array('valid' => false, 'error' => 'ACCESS_TOKEN_EXPIRED');
        }

        // iss가 있으면 검증
        if (isset($payload['iss']) && $payload['iss'] !== 'teamnova-omok') {
            return array('valid' => false, 'error' => 'ACCESS_TOKEN_INVALID');
        }

        return array('valid' => true, 'payload' => $payload);
    }
}
