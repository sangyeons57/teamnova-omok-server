<?php
class UserRepository {
    private $pdo;

    public function __construct($pdo) {
        $this->pdo = $pdo;
    }

    public function findById($userId) {
        $st = $this->pdo->prepare('SELECT user_id, display_name, profile_icon_code, role, status, score FROM teamnova_omok_db.users WHERE user_id = :id LIMIT 1');
        $st->execute(array(':id' => $userId));
        return $st->fetch();
    }

    public function insert($userId, $displayName, $iconCode) {
        $st = $this->pdo->prepare('INSERT INTO teamnova_omok_db.users (user_id, display_name, profile_icon_code) VALUES (:id, :dn, :icon)');
        $st->execute(array(
            ':id' => $userId,
            ':dn' => $displayName,
            ':icon' => ($iconCode === '' ? null : $iconCode),
        ));
    }

    public function updateStatus($userId, $status) {
        $st = $this->pdo->prepare('UPDATE teamnova_omok_db.users SET status = :status WHERE user_id = :id');
        $st->execute(array(
            ':status' => $status,
            ':id' => $userId,
        ));
    }

    public function findByDisplayName($displayName)
    {
        $st = $this->pdo->prepare('SELECT user_id, display_name FROM teamnova_omok_db.users WHERE display_name = :dn LIMIT 1');
        $st->execute(array(':dn' => $displayName));
        $row = $st->fetch();
        return $row ?: null;
    }

    public function updateDisplayName($userId, $displayName): bool
    {
        $st = $this->pdo->prepare('UPDATE teamnova_omok_db.users SET display_name = :dn WHERE user_id = :id');
        $st->execute(array(
            ':dn' => $displayName,
            ':id' => $userId,
        ));
        return $st->rowCount() > 0;
    }

    public function updateProfileIconCode($userId, $iconCode): bool
    {
        $st = $this->pdo->prepare('UPDATE teamnova_omok_db.users SET profile_icon_code = :icon WHERE user_id = :id');
        $st->execute(array(
            ':icon' => $iconCode,
            ':id' => $userId,
        ));
        return $st->rowCount() > 0;
    }

    public function findTopByScore(int $limit, string $status = UserStatus::ACTIVE): array
    {
        $st = $this->pdo->prepare('SELECT user_id, display_name, profile_icon_code, role, status, score FROM teamnova_omok_db.users WHERE status = :status ORDER BY score DESC LIMIT :limit');
        $st->bindValue(':limit', $limit, PDO::PARAM_INT);
        $st->bindValue(':status', $status, PDO::PARAM_STR);
        $st->execute();
        return $st->fetchAll();
    }
}
