<?php
/**
 * 특정 사용자 정보 조회 엔드포인트
 * - Access Token 검증
 * - 요청 본문에서 user_id를 받아 해당 사용자 정보 반환
 */

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
if (!isset($payload['sub']) || trim((string)$payload['sub']) === '') {
    $response->error('ACCESS_TOKEN_INVALID', 401, '유효한 사용자 식별자를 확인할 수 없습니다.');
}

$body = $request->readBody('POST');
$targetUserId = isset($body['user_id']) ? trim((string)$body['user_id']) : '';
if ($targetUserId === '') {
    $response->error('INVALID_USER_ID', 400, '조회할 사용자 ID(user_id)를 입력해 주세요.');
}

$user = $userService->findById($targetUserId);
if ($user === null) {
    $response->error('USER_NOT_FOUND', 404, '사용자를 찾을 수 없습니다.');
}

$profileIcon = isset($user['profile_icon_code']) ? (string)$user['profile_icon_code'] : null;
$score = isset($user['score']) ? (int)$user['score'] : 0;

$response->success(200, array(
    'user' => array(
        'user_id' => (string)$user['user_id'],
        'display_name' => isset($user['display_name']) ? (string)$user['display_name'] : '',
        'profile_icon_code' => $profileIcon,
        'role' => isset($user['role']) ? (string)$user['role'] : null,
        'status' => isset($user['status']) ? (string)$user['status'] : null,
        'score' => $score,
    ),
));
