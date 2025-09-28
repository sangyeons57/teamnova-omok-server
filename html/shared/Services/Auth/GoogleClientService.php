<?php
require_once __DIR__ . '/../../Config.php';

$composerAutoload = __DIR__ . '/../../../vendor/autoload.php';
if (file_exists($composerAutoload)) {
    require_once $composerAutoload;
}

use Google\Client as GoogleClient;

class GoogleClientService
{
    private GoogleClient $client;

    public function __construct(?GoogleClient $client = null)
    {
        $this->client = $client ?? new GoogleClient();

        $webClientId = Config::webClientId();
        if ($webClientId === '') {
            throw new RuntimeException('WEB_CLIENT_ID_NOT_CONFIGURED');
        }

        $this->client->setClientId($webClientId);
    }

    public function verifyIdToken(string $idToken): array
    {
        $payload = $this->client->verifyIdToken($idToken);
        if ($payload === false) {
            throw new RuntimeException('GOOGLE_ID_TOKEN_INVALID');
        }

        return $payload;
    }

    public function getUserId(string $idToken): string
    {
        $payload = $this->verifyIdToken($idToken);
        if (!isset($payload['sub']) || $payload['sub'] === '') {
            throw new RuntimeException('GOOGLE_ID_TOKEN_SUB_MISSING');
        }

        return $payload['sub'];
    }
}
