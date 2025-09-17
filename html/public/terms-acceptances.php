<?php
/**
 * 약관 동의 내역 조회
 * - 인증된 사용자의 현재 동의된 약관 목록을 반환합니다.
 * - 모든 필수 약관에 동의했고 사용자 status가 PENDING이면 ACTIVE로 활성화합니다.
 *
 * 요청(JSON, POST):
 * - access_token: string (Authorization: Bearer ... 사용 가능)
 *
 * 응답:
 * - 200 OK: {
 *     success: true,
 *     accepted_terms: [
 *       { terms_id, terms_type, version, is_required, published_at, accepted_at }, ...
 *     ],
 *     user_status: "ACTIVE" | "PENDING" | "INACTIVE" | "BLOCKED",
 *     activated: true|false
 *   }
 * - 400/401/405/500: 에러
 */
require_once __DIR__ . '/../shared/Database.php';
require_once __DIR__ . '/../shared/Http.php';
require_once __DIR__ . '/../shared/Security/AccessTokenGuard.php';
require_once __DIR__ . '/../shared/Repositories/TermsRepository.php';
require_once __DIR__ . '/../shared/Repositories/UserRepository.php';
require_once __DIR__ . '/../shared/Constants/UserStatus.php';

Http::setJsonResponseHeader();
Http::assertMethod('POST');
$body = Http::readJsonBody();

// 인증 및 페이로드 추출
$payload = AccessTokenGuard::requirePayload($body);
$userId = isset($payload['sub']) ? (string)$payload['sub'] : '';
if ($userId === '') {
    Http::json(401, array(
        'success' => false,
        'error' => 'ACCESS_TOKEN_INVALID',
        'message' => '유효한 사용자 식별자를 확인할 수 없습니다.',
        'retry_with_refresh' => true
    ));
}

$pdo = null;
try {
    $pdo = Database::pdo();
    $pdo->beginTransaction();

    $termsRepo = new TermsRepository($pdo);
    $userRepo  = new UserRepository($pdo);

    // (옵션) terms_type 배열로 동의 기록
    $acceptedCount = null;
    if (isset($body['accept_types'])) {
        if (!is_array($body['accept_types'])) {
            $pdo->rollBack();
            Http::json(400, array(
                'success' => false,
                'error' => 'INVALID_ACCEPT_TYPES',
                'message' => 'accept_types는 terms_type 문자열 배열이어야 합니다.'
            ));
        }
        $result = $termsRepo->acceptByTypes($userId, $body['accept_types']);
        $acceptedCount = isset($result['accepted_count']) ? (int)$result['accepted_count'] : 0;
    }

    // 사용자가 동의한 약관 목록
    $accepted = $termsRepo->findAcceptedByUserId($userId);

    // 모든 필수 약관 동의 시 PENDING -> ACTIVE 승격
    $activated = false;
    $user = $userRepo->findById($userId);
    $currentStatus = $user && isset($user['status']) ? (string)$user['status'] : null;

    if ($currentStatus === UserStatus::PENDING) {
        $allRequiredAccepted = $termsRepo->allRequiredAcceptedByUserId($userId);
        if ($allRequiredAccepted) {
            $userRepo->updateStatus($userId, UserStatus::ACTIVE);
            $currentStatus = UserStatus::ACTIVE;
            $activated = true;
        }
    }

    $pdo->commit();

    $resp = array(
        'success' => true,
        'accepted_terms' => $accepted,
        'user_status' => $currentStatus,
        'activated' => $activated,
    );
    if ($acceptedCount !== null) {
        $resp['accepted_count'] = $acceptedCount;
    }

    Http::json(200, $resp);
} catch (PDOException $e) {
    if ($pdo && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    Http::exceptionDbError('데이터베이스 오류가 발생했습니다.', $e);
} catch (Exception $e) {
    if ($pdo && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    Http::exceptionInternal('약관 동의 내역을 조회하는 중 오류가 발생했습니다.', $e);
}
