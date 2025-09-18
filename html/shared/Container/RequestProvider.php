<?php
require_once __DIR__ . '/../Services/Http/ResponseService.php';
require_once __DIR__ . '/../Services/Http/RequestService.php';

class RequestProvider implements ServiceProvider
{
    public function register(Container $c): void
    {
        $c->set(RequestService::class, function (Container $c) {
            $response = $c->get(ResponseService::class);
            return new RequestService($response);
        });
    }
}
