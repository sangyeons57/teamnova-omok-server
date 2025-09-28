<?php
require_once __DIR__ . '/Container.php';
require_once __DIR__ . '/ServiceProvider.php';
require_once __DIR__ . '/../Repositories/RefreshTokenRepository.php';
require_once __DIR__ . '/../Repositories/UserRepository.php';
require_once __DIR__ . '/../Services/Util/CryptoService.php';
require_once __DIR__ . '/../Services/Util/ClockService.php';
require_once __DIR__ . '/../Services/Auth/TokenService.php';
require_once __DIR__ . '/../Services/Auth/GoogleClientService.php';

class AuthServiceProvider implements ServiceProvider
{
    public function register(Container $c): void
    {
        $c->set(TokenService::class, function (Container $c) {
            return new TokenService(
                $c->get(RefreshTokenRepository::class),
                $c->get(UserRepository::class),
                $c->get(CryptoService::class),
                $c->get(ClockService::class)
            );
        });

        $c->set(GoogleClientService::class, function () {
            return new GoogleClientService();
        });
    }
}
