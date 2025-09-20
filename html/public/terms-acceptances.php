<?php
/**
 * 약관 동의 내역 조회
 * - 인증된 사용자의 현재 동의된 약관 목록을 반환합니다.
 * - 모든 필수 약관에 동의했고 사용자 status가 PENDING이면 ACTIVE로 활성화합니다.
 *
 * 요청(JSON, POST):
 * - access_token: string (Authorization: Bearer ... 사용 가능)
 */
require_once __DIR__ . '/../shared/Database.php';
require_once __DIR__ . '/../shared/Repositories/TermsRepository.php';
require_once __DIR__ . '/../shared/Repositories/UserRepository.php';
require_once __DIR__ . '/../shared/Constants/UserStatus.php';
require_once __DIR__ . '/../shared/Container/Container.php';
require_once __DIR__ . '/../shared/Container/ServiceProvider.php';
require_once __DIR__ . '/../shared/Container/AppProvider.php';
require_once __DIR__ . '/../shared/Container/UtilProvider.php';
require_once __DIR__ . '/../shared/Container/AuthProvider.php';
require_once __DIR__ . '/../shared/Container/ResponseProvider.php';
require_once __DIR__ . '/../shared/Container/RequestProvider.php';
require_once __DIR__ . '/../shared/Container/GuardProvider.php';

$container = new Container();
(new AppProvider())->register($container);
(new UtilProvider())->register($container);
(new AuthProvider())->register($container);
(new ResponseProvider())->register($container);
(new RequestProvider())->register($container);
(new GuardProvider())->register($container);
/** @var ResponseService $response */
$response = $container->get(ResponseService::class);
/** @var RequestService $requestService */
$requestService = $container->get(RequestService::class);

// 공통 시작부
$envelope = $requestService->readEnvelope('POST');
$body = $envelope['body'];
// 인증 및 페이로드 추출
/** @var AccessTokenGuardService $guard */
$guard = $container->get(AccessTokenGuardService::class);
$payload = $guard->requirePayload($body);
$userId = isset($payload['sub']) ? (string)$payload['sub'] : '';
if ($userId === '') {
    $response->error('ACCESS_TOKEN_INVALID', 401, '유효한 사용자 식별자를 확인할 수 없습니다.');
}

$pdo = null;
try {
    $pdo = Database::pdo();
    $pdo->beginTransaction();

    /** @var TermsRepository $termsRepo */
    $termsRepo = $container->get(TermsRepository::class);
    /** @var UserRepository $userRepo */
    $userRepo  = $container->get(UserRepository::class);

    // (옵션) terms_type 배열로 동의 기록
    $acceptedCount = null;
    if (isset($body['accept_types'])) {
        if (!is_array($body['accept_types'])) {
            $pdo->rollBack();
            $response->error('INVALID_ACCEPT_TYPES', 400, 'accept_types는 terms_type 문자열 배열이어야 합니다.');
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

    $payloadOut = array(
        'accepted_terms' => $accepted,
        'user_status' => $currentStatus,
        'activated' => $activated,
    );
    if ($acceptedCount !== null) {
        $payloadOut['accepted_count'] = $acceptedCount;
    }

    $response->success(200, 'terms_acceptances', $userId, $payloadOut);
} catch (PDOException $e) {
    if ($pdo && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    $response->exceptionDbError('데이터베이스 오류가 발생했습니다.', $e);
} catch (Exception $e) {
    if ($pdo && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    $response->exceptionInternal('약관 동의 내역을 조회하는 중 오류가 발생했습니다.', $e);
}
