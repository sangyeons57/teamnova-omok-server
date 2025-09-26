<?php
final class Config
{
    private const DOTENV_PATH = '/var/www/app/.env';
    private static array $cfg;

    private static function load(): void
    {
        if (isset(self::$cfg)) {
            return;
        }

        $env = $_ENV + $_SERVER;

        if (is_readable(self::DOTENV_PATH)) {
            $lines = file(self::DOTENV_PATH, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
            if ($lines !== false) {
                foreach ($lines as $line) {
                    $line = trim($line);
                    if ($line === '' || $line[0] === '#' || $line[0] === ';') {
                        continue;
                    }

                    $delimiter = strpos($line, '=');
                    if ($delimiter === false) {
                        continue;
                    }

                    $name = trim(substr($line, 0, $delimiter));
                    if ($name === '') {
                        continue;
                    }

                    $value = trim(substr($line, $delimiter + 1));
                    if ($value !== '' && $value[0] === '"' && substr($value, -1) === '"') {
                        $value = substr($value, 1, -1);
                    }

                    $env[$name] = $value;
                }
            }
        }

        self::$cfg = [
            'APP_ENV' => $env['APP_ENV'] ?? 'local',
            'APP_DEBUG' => isset($env['APP_DEBUG']) && in_array(strtolower((string)$env['APP_DEBUG']), ['1', 'true', 'yes', 'on'], true),
            'JWT_SECRET' => $env['JWT_SECRET'] ?? 'dev-secret-change-me',
            'ACCESS_TTL_SEC' => isset($env['ACCESS_TTL_SEC']) ? (int)$env['ACCESS_TTL_SEC'] : 3600,
            'REFRESH_TTL_DAYS' => isset($env['REFRESH_TTL_DAYS']) ? (int)$env['REFRESH_TTL_DAYS'] : 14,
            'DB_HOST' => $env['DB_HOST'] ?? '127.0.0.1',
            'DB_PORT' => isset($env['DB_PORT']) ? (int)$env['DB_PORT'] : 3306,
            'DB_NAME' => $env['DB_NAME'] ?? 'teamnova_omok_db',
            'DB_USER' => $env['DB_USER'] ?? 'root',
            'DB_PASS' => $env['DB_PASS'] ?? '',
            'DB_DSN' => $env['DB_DSN'] ?? '',
            'DB_CHARSET' => $env['DB_CHARSET'] ?? 'utf8mb4',
        ];
    }

    public static function appEnv(): string
    {
        self::load();
        return self::$cfg['APP_ENV'];
    }

    public static function appDebug(): bool
    {
        self::load();
        return self::$cfg['APP_DEBUG'];
    }

    public static function jwtSecret(): string
    {
        self::load();
        return self::$cfg['JWT_SECRET'];
    }

    public static function accessTtlSec(): int
    {
        self::load();
        return self::$cfg['ACCESS_TTL_SEC'];
    }

    public static function refreshTtlDays(): int
    {
        self::load();
        return self::$cfg['REFRESH_TTL_DAYS'];
    }

    public static function dbHost(): string
    {
        self::load();
        return self::$cfg['DB_HOST'];
    }

    public static function dbPort(): int
    {
        self::load();
        return self::$cfg['DB_PORT'];
    }

    public static function dbName(): string
    {
        self::load();
        return self::$cfg['DB_NAME'];
    }

    public static function dbUser(): string
    {
        self::load();
        return self::$cfg['DB_USER'];
    }

    public static function dbPass(): string
    {
        self::load();
        return self::$cfg['DB_PASS'];
    }

    public static function dbCharset(): string
    {
        self::load();
        return self::$cfg['DB_CHARSET'];
    }

    public static function dbDsn(): string
    {
        self::load();
        if (self::$cfg['DB_DSN'] !== '') {
            return self::$cfg['DB_DSN'];
        }

        return sprintf(
            'mysql:host=%s;port=%d;dbname=%s;charset=%s',
            self::$cfg['DB_HOST'],
            self::$cfg['DB_PORT'],
            self::$cfg['DB_NAME'],
            self::$cfg['DB_CHARSET']
        );
    }
}
