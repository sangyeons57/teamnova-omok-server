<?php
class RefreshTokenRepository {
    private $pdo;

    public function __construct($pdo) {
        $this->pdo = $pdo;
    }

    public function save($userId, $tokenHash, $expiresAt) {
        $st = $this->pdo->prepare('INSERT INTO refresh_tokens (user_id, token_hash, expires_at) VALUES (:uid, :th, :exp)');
        $st->execute(array(
            ':uid' => $userId,
            ':th'  => $tokenHash,
            ':exp' => $expiresAt->format('Y-m-d H:i:s'),
        ));
    }

    public function findByHash($tokenHash) {
        $st = $this->pdo->prepare('SELECT user_id, expires_at, revoked_at FROM refresh_tokens WHERE token_hash = :th LIMIT 1');
        $st->execute(array(':th' => $tokenHash));
        $row = $st->fetch(PDO::FETCH_ASSOC);
        return $row ? $row : null;
    }

    public function revokeByHash($tokenHash) {
        $nowUtc = new DateTime('now', new DateTimeZone('UTC'));
        $st = $this->pdo->prepare('UPDATE refresh_tokens SET revoked_at = :now WHERE token_hash = :th AND revoked_at IS NULL');
        $st->execute(array(
            ':now' => $nowUtc->format('Y-m-d H:i:s'),
            ':th'  => $tokenHash,
        ));
    }

    public function revokeAllByUserId($userId) {
        $nowUtc = new DateTime('now', new DateTimeZone('UTC'));
        $st = $this->pdo->prepare('UPDATE refresh_tokens SET revoked_at = :now WHERE user_id = :uid AND revoked_at IS NULL');
        $st->execute(array(
            ':now' => $nowUtc->format('Y-m-d H:i:s'),
            ':uid' => $userId,
        ));
    }

}
