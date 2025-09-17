<?php
class Config {
    public static function dbHost(): false|array|string
    { return getenv('DB_HOST') !== false ? getenv('DB_HOST') : '127.0.0.1'; }
    public static function dbName(): false|array|string
    { return getenv('DB_NAME') !== false ? getenv('DB_NAME') : 'teamnova_omok_db'; }
    public static function dbUser(): false|array|string
    { return getenv('DB_USER') !== false ? getenv('DB_USER') : 'root'; }
    public static function dbPass(): false|array|string
    { return getenv('DB_PASS') !== false ? getenv('DB_PASS') : ''; }

    public static function jwtSecret(): false|array|string
    { return getenv('JWT_SECRET') !== false ? getenv('JWT_SECRET') : 'dev-secret-change-me'; }

    public static function accessTtlSec(): int
    { return 3600; }   // 1 hour
    public static function refreshTtlDays(): int
    { return 14; }   // 14 days
}
