<?php
require_once __DIR__ . '/Config.php';

class Database {
    public static function pdo(): PDO{
        $options = [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ];

        return new PDO(
            Config::dbDsn(),
            Config::dbUser(),
            Config::dbPass(),
            $options
        );
    }
}
