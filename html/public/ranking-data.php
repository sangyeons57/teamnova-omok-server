<?php
/**
 * 랭킹 데이터 조회 엔드포인트
 * - score 내림차순으로 최대 500명까지 반환
 * - 요청(JSON, POST) 본문에서 limit(선택)를 지정해 1~500 범위로 제한
 */

require_once __DIR__ . '/../shared/Container/ContainerFactory.php';

$container = ContainerFactory::create();
/** @var ResponseService $response */
$response = $container->get(ResponseService::class);
/** @var RequestService $request */
$request = $container->get(RequestService::class);
/** @var UserService $userService */
$userService = $container->get(UserService::class);

$body = $request->readBody('POST');
$limit = 500;
if (array_key_exists('limit', $body)) {
    $limitParam = $body['limit'];
    if (is_int($limitParam)) {
        $limit = $limitParam;
    } elseif (is_string($limitParam) && preg_match('/^[0-9]+$/', $limitParam)) {
        $limit = (int)$limitParam;
    } elseif (is_numeric($limitParam)) {
        $limit = (int)$limitParam;
    } else {
        $response->error('INVALID_LIMIT', 400, 'limit 값은 1 이상 500 이하의 정수여야 합니다.');
    }
}

if ($limit < 1) {
    $limit = 1;
}
if ($limit > 500) {
    $limit = 500;
}

try {
    $rows = $userService->findTopByScore($limit);
    $rank = 1;
    $items = array();
    foreach ($rows as $row) {
        $items[] = array(
            'rank' => $rank,
            'user_id' => isset($row['user_id']) ? (string)$row['user_id'] : '',
            'display_name' => isset($row['display_name']) ? (string)$row['display_name'] : '',
            'score' => isset($row['score']) ? (int)$row['score'] : 0,
        );
        $rank++;
    }

    $response->success(200, array(
        'limit' => $limit,
        'count' => count($items),
        'ranking' => $items,
    ));
} catch (PDOException $e) {
    $response->exceptionDbError('랭킹 데이터를 조회하는 중 오류가 발생했습니다.', $e);
} catch (Exception $e) {
    $response->exceptionInternal('랭킹 데이터를 처리하는 중 내부 오류가 발생했습니다.', $e);
}
