<?php
class AuthProviderRepository {
    private $pdo;

    public function __construct($pdo) {
        $this->pdo = $pdo;
    }

    public function findUserId($provider, $providerUserId) {
        $st = $this->pdo->prepare('SELECT user_id FROM teamnova_omok_db.auth_providers WHERE provider = :p AND provider_user_id = :pid LIMIT 1');
        $st->execute(array(':p' => $provider, ':pid' => $providerUserId));
        $row = $st->fetch();
        return $row ? $row['user_id'] : null;
    }

    public function insert($userId, $provider, $providerUserId) {
        $st = $this->pdo->prepare('INSERT INTO teamnova_omok_db.auth_providers (user_id, provider, provider_user_id) VALUES (:uid, :p, :pid)');
        $st->execute(array(
            ':uid' => $userId,
            ':p' => $provider,
            ':pid' => $providerUserId,
        ));
    }

    public function findByUserId($userId)
    {
        $st = $this->pdo->prepare('SELECT provider, provider_user_id FROM teamnova_omok_db.auth_providers WHERE user_id = :uid LIMIT 1');
        $st->execute(array(':uid' => $userId));
        $row = $st->fetch();
        return $row ?: null;
    }
}
