<?php
require_once __DIR__ . '/../Repositories/UserRepository.php';
require_once __DIR__ . '/../Repositories/AuthProviderRepository.php';
require_once __DIR__ . '/../Constants/UserStatus.php';

class UserService
{
    private UserRepository $users;
    private AuthProviderRepository $authProviders;

    public function __construct(UserRepository $users, AuthProviderRepository $authProviders)
    {
        $this->users = $users;
        $this->authProviders = $authProviders;
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

    public function findByDisplayName(string $displayName): ?array
    {
        if ($displayName === '') {
            return null;
        }

        $row = $this->users->findByDisplayName($displayName);
        if ($row === false) {
            return null;
        }

        return $row;
    }

    public function updateDisplayName(string $userId, string $displayName): bool
    {
        if ($userId === '' || $displayName === '') {
            return false;
        }

        return $this->users->updateDisplayName($userId, $displayName);
    }

    public function updateProfileIconCode(string $userId, string $iconCode): bool
    {
        if ($userId === '' || $iconCode === '') {
            return false;
        }

        return $this->users->updateProfileIconCode($userId, $iconCode);
    }

    public function findTopByScore(int $limit): array
    {
        if ($limit <= 0) {
            return array();
        }

        return $this->users->findTopByScore($limit);
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

        $this->authProviders->deleteByUserId($userId);

        if (isset($row['status']) && (string)$row['status'] === UserStatus::INACTIVE) {
            return true; // 이미 비활성화됨(멱등)
        }

        $this->users->updateStatus($userId, UserStatus::INACTIVE);
        return true;
    }
}
