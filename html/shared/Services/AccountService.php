<?php
require_once __DIR__ . '/../Repositories/UserRepository.php';
require_once __DIR__ . '/../Repositories/AuthProviderRepository.php';
require_once __DIR__ . '/../Services/Util/UuidService.php';

class AccountService {
    private PDO $pdo;
    private UserRepository $users;
    private AuthProviderRepository $auth;
    private UuidService $uuid;

    public function __construct($pdo, $users, $auth, $uuid) {
        $this->pdo = $pdo;
        $this->users = $users;
        $this->auth = $auth;
        $this->uuid = $uuid;
    }

    // 반환: array('created' => bool, 'user' => row)
    public function createOrGetUser($provider, $providerUserId, $displayName, $iconCode): array
    {
        // provider_user_id가 빈 문자열이면 null로 정규화(일반 처리)
        $pid = ($providerUserId === '' ? null : $providerUserId);

        $userId = null;
        if ($pid !== null) {
            $userId = $this->auth->findUserId($provider, $pid);
        }

        if ($userId) {
            $row = $this->users->findById($userId);
            if (!$row) {
                throw new Exception('DATA_INTEGRITY_ERROR');
            }
            return array('created' => false, 'user' => $row);
        }

        try {
            $this->pdo->beginTransaction();

            $newId = $this->uuid->v4();
            $this->users->insert($newId, $displayName, $iconCode);
            // provider_user_id는 null일 수 있음(예: GUEST)
            $this->auth->insert($newId, $provider, $pid);

            $this->pdo->commit();

            $row = $this->users->findById($newId);
            return array('created' => true, 'user' => $row);
        } catch (Exception $e) {
            if ($this->pdo->inTransaction()) {
                $this->pdo->rollBack();
            }
            throw $e;
        }
    }
}
