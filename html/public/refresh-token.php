<?php
/**
 * 토큰 재발급(Refresh) 엔드포인트
 * 입력(JSON, POST):
 * - refresh_token: string (필수)
 */
require_once __DIR__ . '/../shared/Container/ContainerFactory.php';

$container = ContainerFactory::create();
/** @var ResponseService $response */
$response = $container->get(ResponseService::class);
/** @var RequestService $requestService */
$requestService = $container->get(RequestService::class);

// 공통 시작부
$body = $requestService->readBody('POST');

$refreshToken = isset($body['refresh_token']) ? trim($body['refresh_token']) : '';
if ($refreshToken === '') {
    $response->error('INVALID_REFRESH_TOKEN', 400, 'refresh_token이 비어있습니다.');
}

$pdo = null;
try {
    /** @var PDO $pdo */
    $pdo = $container->get(PDO::class);

    /** @var TokenService $tokenService */
    $tokenService = $container->get(TokenService::class);
    $tokens = $tokenService->refresh($refreshToken);

    $response->success(200, $tokens);

} catch (Exception $e) {
    $code = $e->getMessage();

    if ($code === 'INVALID_REFRESH_TOKEN') {
        $response->error('INVALID_REFRESH_TOKEN', 401, '리프레시 토큰이 유효하지 않습니다. 다시 로그인해 주세요.');
    } else if ($code === 'REFRESH_TOKEN_EXPIRED') {
        $response->error('REFRESH_TOKEN_EXPIRED', 401, '리프레시 토큰이 만료되었습니다. 다시 로그인해 주세요.');
    } else if ($code === 'REFRESH_TOKEN_REVOKED') {
        $response->error('REFRESH_TOKEN_REVOKED', 401, '이미 사용되었거나 취소된 리프레시 토큰입니다. 다시 로그인해 주세요.');
    } else {
        $response->exceptionInternal('내부 오류가 발생했습니다.', $e);
    }
}
