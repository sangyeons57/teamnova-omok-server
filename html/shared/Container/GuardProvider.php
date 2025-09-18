<?php
require_once __DIR__ . '/../Services/Guard/AccessTokenGuardService.php';
require_once __DIR__ . '/../Services/Auth/TokenService.php';
require_once __DIR__ . '/../Services/Http/ResponseService.php';

class GuardProvider implements ServiceProvider
{
    public function register(Container $c): void
    {
        $c->set(AccessTokenGuardService::class, function (Container $c) {
            $tokenService = $c->get(TokenService::class);
            $response = $c->get(ResponseService::class);
            return new AccessTokenGuardService($tokenService, $response);
        });
    }
}
