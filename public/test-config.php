<?php
declare(strict_types=1);

require_once __DIR__ . '/../html/shared/Config.php';
require_once __DIR__ . '/../html/shared/Database.php';

$values = [
    'APP_ENV' => Config::appEnv(),
    'APP_DEBUG' => Config::appDebug() ? 'true' : 'false',
    'JWT_SECRET' => Config::jwtSecret(),
    'ACCESS_TTL_SEC' => (string)Config::accessTtlSec(),
    'REFRESH_TTL_DAYS' => (string)Config::refreshTtlDays(),
    'DB_HOST' => Config::dbHost(),
    'DB_PORT' => (string)Config::dbPort(),
    'DB_NAME' => Config::dbName(),
    'DB_USER' => Config::dbUser(),
    'DB_PASS' => Config::dbPass(),
    'DB_CHARSET' => Config::dbCharset(),
    'DB_DSN' => Config::dbDsn(),
];

$dbStatus = [
    'connected' => false,
    'server_version' => null,
    'database_name' => null,
    'error' => null,
];

try {
    $pdo = Database::pdo();
    $dbStatus['connected'] = true;
    $dbStatus['server_version'] = (string)$pdo->getAttribute(PDO::ATTR_SERVER_VERSION);
    $dbStatus['database_name'] = (string)$pdo->query('SELECT DATABASE()')->fetchColumn();
} catch (Throwable $e) {
    $dbStatus['error'] = $e->getMessage();
}

header('Content-Type: text/plain; charset=UTF-8');

echo "Loaded Config Values\n";
echo str_repeat('=', 21) . "\n\n";
foreach ($values as $key => $value) {
    echo $key . ': ' . $value . "\n";
}

echo "\nDatabase Connection Test\n";
echo str_repeat('=', 26) . "\n\n";

echo 'connected: ' . ($dbStatus['connected'] ? 'true' : 'false') . "\n";
if ($dbStatus['error'] !== null) {
    echo 'error: ' . $dbStatus['error'] . "\n";
} else {
    echo 'server_version: ' . $dbStatus['server_version'] . "\n";
    echo 'database_name: ' . $dbStatus['database_name'] . "\n";
}
