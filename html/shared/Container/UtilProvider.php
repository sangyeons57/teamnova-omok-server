<?php
require_once __DIR__ . '/../Services/Util/ClockService.php';
require_once __DIR__ . '/../Services/Util/CryptoService.php';
require_once __DIR__ . '/../Services/Util/NormalizeService.php';
require_once __DIR__ . '/../Services/Util/UuidService.php';
require_once __DIR__ . '/../Services/Util/ValidationService.php';
require_once __DIR__ . '/../Services/Util/StopwatchService.php';

class UtilProvider implements ServiceProvider
{
    public function register(Container $c): void
    {
        $c->set(ClockService::class, function () {
            return new ClockService();
        });
        $c->set(CryptoService::class, function () {
            return new CryptoService();
        });
        $c->set(NormalizeService::class, function () {
            return new NormalizeService();
        });
        $c->set(UuidService::class, function () {
            return new UuidService();
        });
        $c->set(ValidationService::class, function () {
            return new ValidationService();
        });
        $c->set(StopwatchService::class, function () {
            return new StopwatchService();
        });
    }
}
