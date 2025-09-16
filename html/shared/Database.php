<?php
require_once __DIR__ . '/Config.php';

class Database {
    public static function pdo(): PDO{
        $dsn = 'mysql:host=' . Config::dbHost() . ';dbname=' . Config::dbName() . ';charset=utf8mb4';
        $options = [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ];
        return new PDO($dsn, Config::dbUser(), Config::dbPass(), $options);
    }
}
