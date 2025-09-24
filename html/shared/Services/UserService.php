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

    /**
     * 사용자를 비활성화(INACTIVE) 상태로 전환합니다.
     * 존재하지 않는 경우 false, 이미 비활성화된 경우 true를 반환하여 멱등성을 보장합니다.
     */
    public function deactivate(string $userId): bool
    {
        if ($userId === '') {
            return false;
        }

        $row = $this->findById($userId);
        if ($row === null) {
            return false;
        }

        if (isset($row['status']) && (string)$row['status'] === UserStatus::INACTIVE) {
            return true; // 이미 비활성화됨(멱등)
        }

        $this->users->updateStatus($userId, UserStatus::INACTIVE);
        return true;
    }
}
