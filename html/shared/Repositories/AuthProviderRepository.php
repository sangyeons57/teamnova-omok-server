<?php
class AuthProviderRepository {
    private $pdo;

    public function __construct($pdo) {
        $this->pdo = $pdo;
    }

    public function findUserId($provider, $providerUserId) {
        $st = $this->pdo->prepare('SELECT user_id FROM teamnova_omok_db.auth_providers WHERE provider = :p AND provider_user_id = :pid ORDER BY linked_at DESC LIMIT 1');
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

        $st = $this->pdo->prepare('SELECT provider, provider_user_id, linked_at FROM teamnova_omok_db.auth_providers WHERE user_id = :uid ORDER BY linked_at DESC LIMIT 1');
        $st->execute(array(':uid' => $userId));
        $row = $st->fetch();
        return $row ?: null;
    }

    public function upsertProviderUserId(string $userId, string $provider, ?string $providerUserId): void
    {
        $normalizedProviderUserId = ($providerUserId === '' ? null : $providerUserId);

        $update = $this->pdo->prepare('UPDATE teamnova_omok_db.auth_providers SET provider_user_id = :pid, linked_at = NOW() WHERE user_id = :uid AND provider = :p');
        $update->execute(array(
            ':pid' => $normalizedProviderUserId,
            ':uid' => $userId,
            ':p' => $provider,
        ));

        if ($update->rowCount() > 0) {
            return;
        }

        $insert = $this->pdo->prepare('INSERT INTO teamnova_omok_db.auth_providers (user_id, provider, provider_user_id, linked_at) VALUES (:uid, :p, :pid, NOW())');
        $insert->execute(array(
            ':uid' => $userId,
            ':p' => $provider,
            ':pid' => $normalizedProviderUserId,
        ));
    }

    public function findByUserIdAndProvider(string $userId, string $provider): ?array
    {
        $st = $this->pdo->prepare('SELECT user_id, provider, provider_user_id, linked_at FROM teamnova_omok_db.auth_providers WHERE user_id = :uid AND provider = :p LIMIT 1');
        $st->execute(array(
            ':uid' => $userId,
            ':p' => $provider,
        ));
        $row = $st->fetch();
        return $row ?: null;
    }

    public function deleteByUserId(string $userId): void
    {
        $st = $this->pdo->prepare('DELETE FROM teamnova_omok_db.auth_providers WHERE user_id = :uid');
        $st->execute(array(':uid' => $userId));
    }
}
