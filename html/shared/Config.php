<?php
class Config {
    public static function dbHost() { return getenv('DB_HOST') !== false ? getenv('DB_HOST') : '127.0.0.1'; }
    public static function dbName() { return getenv('DB_NAME') !== false ? getenv('DB_NAME') : 'teamnova_omok_db'; }
    public static function dbUser() { return getenv('DB_USER') !== false ? getenv('DB_USER') : 'root'; }
    public static function dbPass() { return getenv('DB_PASS') !== false ? getenv('DB_PASS') : ''; }

    public static function jwtSecret() { return getenv('JWT_SECRET') !== false ? getenv('JWT_SECRET') : 'dev-secret-change-me'; }

    public static function accessTtlSec() { return 3600; }   // 1 hour
    public static function refreshTtlDays() { return 14; }   // 14 days
}
