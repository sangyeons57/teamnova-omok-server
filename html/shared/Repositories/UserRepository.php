<?php
class UserRepository {
    private $pdo;

    public function __construct($pdo) {
        $this->pdo = $pdo;
    }

    public function findById($userId) {
        $st = $this->pdo->prepare('SELECT user_id, display_name, profile_icon_code, role, status, score FROM users WHERE user_id = :id LIMIT 1');
        $st->execute(array(':id' => $userId));
        return $st->fetch();
    }

    public function insert($userId, $displayName, $iconCode) {
        $st = $this->pdo->prepare('INSERT INTO users (user_id, display_name, profile_icon_code) VALUES (:id, :dn, :icon)');
        $st->execute(array(
            ':id' => $userId,
            ':dn' => $displayName,
            ':icon' => ($iconCode === '' ? null : $iconCode),
        ));
    }

    public function updateStatus($userId, $status) {
        $st = $this->pdo->prepare('UPDATE users SET status = :status WHERE user_id = :id');
        $st->execute(array(
            ':status' => $status,
            ':id' => $userId,
        ));
    }
}
