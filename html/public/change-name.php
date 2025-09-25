<?php
/**
 * 사용자 이름 변경 엔드포인트
 * - Access Token 검증 필수
 * - display_name이 고유하도록 검증 후 갱신
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
/** @var ValidationService $validator */
$validator = $container->get(ValidationService::class);
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
$newName = isset($body['new_name']) ? $body['new_name'] : null;
if (!is_string($newName)) {
    $response->error('INVALID_DISPLAY_NAME', 400, '변경할 사용자 이름(new_name)을 문자열로 전달해 주세요.');
}

$displayName = trim($newName);
if ($displayName === '') {
    $response->error('INVALID_DISPLAY_NAME', 400, '변경할 사용자 이름을 입력해 주세요.');
}

$length = $validator->mbLength($displayName);
if ($length < 2 || $length > 30) {
    $response->error('INVALID_DISPLAY_NAME', 400, '사용자 이름은 2자 이상 30자 이하로 입력해 주세요.');
}

$currentName = isset($user['display_name']) ? (string)$user['display_name'] : '';
if ($currentName === $displayName) {
    $response->success(200, array(
        'updated' => false,
        'display_name' => $currentName,
        'unchanged' => true,
    ));
}

$duplicate = $userService->findByDisplayName($displayName);
if ($duplicate !== null && isset($duplicate['user_id']) && (string)$duplicate['user_id'] !== $userId) {
    $response->error('DISPLAY_NAME_ALREADY_TAKEN', 409, '이미 사용 중인 사용자 이름입니다.');
}

try {
    $updated = $userService->updateDisplayName($userId, $displayName);
    $response->success(200, array(
        'updated' => $updated,
        'display_name' => $displayName,
        'unchanged' => !$updated,
    ));
} catch (PDOException $e) {
    $response->exceptionDbError('사용자 이름 변경 중 오류가 발생했습니다.', $e);
}
