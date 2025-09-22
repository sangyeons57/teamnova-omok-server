<?php
require_once __DIR__ . '/../Repositories/UserRepository.php';
require_once __DIR__ . '/../Constants/UserStatus.php';

class UserService
{
    private UserRepository $users;

    public function __construct(UserRepository $users)
    {
        $this->users = $users;
    }

    /**
     * @return array|null PDO::FETCH_ASSOC row or null when not found
     */
    public function findById(string $userId): ?array
    {
        if ($userId === '') {
            return null;
        }
        $row = $this->users->findById($userId);
        if ($row === false) {
            return null;
        }
        return $row;
    }

    public function isActive(string $userId): bool
    {
        $row = $this->findById($userId);
        return $row !== null && isset($row['status']) && (string)$row['status'] === UserStatus::ACTIVE;
    }
}
