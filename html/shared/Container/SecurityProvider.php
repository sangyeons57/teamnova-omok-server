<?php
require_once __DIR__ . '/Container.php';
require_once __DIR__ . '/ServiceProvider.php';
require_once __DIR__ . '/../Services/Guard/AccessTokenGuardService.php';
require_once __DIR__ . '/../Services/Auth/TokenService.php';
require_once __DIR__ . '/../Services/Http/ResponseService.php';

class SecurityProvider implements ServiceProvider
{
    public function register(Container $c): void
    {
        $c->set(AccessTokenGuardService::class, function (Container $c) {
            return new AccessTokenGuardService(
                $c->get(TokenService::class),
                $c->get(ResponseService::class)
            );
        });
    }
}
