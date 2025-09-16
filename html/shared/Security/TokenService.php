<?php
require_once __DIR__ . '/../Config.php';
require_once __DIR__ . '/../Util/Crypto.php';
require_once __DIR__ . '/../Util/Clock.php';
require_once __DIR__ . '/Jwt.php';

class TokenService {
    private $refreshRepo;

    public function __construct($refreshRepo) {
        $this->refreshRepo = $refreshRepo;
    }

    public function issue($userId, $provider, $role) {
        $now = time();
        $accessExp = $now + Config::accessTtlSec();

        $jwt = Jwt::encodeHS256(array(
            'iss' => 'teamnova-omok',
            'sub' => $userId,
            'provider' => $provider,
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
}
