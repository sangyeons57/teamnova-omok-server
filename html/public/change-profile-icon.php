<?php
/**
 * 사용자 프로필 아이콘 변경 엔드포인트
 * - Access Token 검증 필수
 * - profile_icon_code는 0~9 범위의 한 자리 숫자만 허용
 */

require_once __DIR__ . '/../shared/Constants/UserStatus.php';
require_once __DIR__ . '/../shared/Container/ContainerFactory.php';

$container = ContainerFactory::create();
/** @var ResponseService $response */
$response = $container->get(ResponseService::class);
/** @var RequestService $request */
$request = $container->get(RequestService::class);
/** @var AccessTokenGuardService $guard */
$guard = $container->get(AccessTokenGuardService::class);
/** @var UserService $userService */
$userService = $container->get(UserService::class);

$payload = $guard->requirePayload();
$userId = isset($payload['sub']) ? (string)$payload['sub'] : '';
if ($userId === '') {
    $response->error('ACCESS_TOKEN_INVALID', 401, '유효한 사용자 식별자를 확인할 수 없습니다.');
}

$user = $userService->findById($userId);
if ($user === null) {
    $response->error('USER_NOT_FOUND', 404, '사용자를 찾을 수 없습니다.');
}

$status = isset($user['status']) ? (string)$user['status'] : '';
if ($status !== UserStatus::ACTIVE) {
    $response->error('USER_NOT_ACTIVE', 403, '사용자 상태가 활성화되어 있지 않습니다.');
}

$body = $request->readBody('POST');
$iconValue = array_key_exists('new_icon', $body) ? $body['new_icon'] : null;
if ($iconValue === null) {
    $response->error('INVALID_PROFILE_ICON_CODE', 400, '변경할 프로필 아이콘(new_icon)을 입력해 주세요.');
}

if (is_int($iconValue)) {
    $numericIcon = $iconValue;
} elseif (is_string($iconValue) && preg_match('/^[0-9]$/', trim($iconValue))) {
    $numericIcon = (int)trim($iconValue);
} else {
    $response->error('INVALID_PROFILE_ICON_CODE', 400, '프로필 아이콘(new_icon)은 0부터 9까지의 정수여야 합니다.');
}

if ($numericIcon < 0 || $numericIcon > 9) {
    $response->error('INVALID_PROFILE_ICON_CODE', 400, '프로필 아이콘(new_icon)은 0부터 9까지의 정수여야 합니다.');
}

$profileIconCode = (string)$numericIcon;

$currentIcon = isset($user['profile_icon_code']) ? (string)$user['profile_icon_code'] : '';
if ($currentIcon === $profileIconCode) {
    $response->success(200, array(
        'updated' => false,
        'profile_icon_code' => $profileIconCode,
        'unchanged' => true,
    ));
}

try {
    $updated = $userService->updateProfileIconCode($userId, $profileIconCode);
    $response->success(200, array(
        'updated' => $updated,
        'profile_icon_code' => $profileIconCode,
        'unchanged' => !$updated,
    ));
} catch (PDOException $e) {
    $response->exceptionDbError('프로필 아이콘 변경 중 오류가 발생했습니다.', $e);
}
