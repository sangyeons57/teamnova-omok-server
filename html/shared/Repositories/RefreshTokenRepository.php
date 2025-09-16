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
}
