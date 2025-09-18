<?php
require_once __DIR__ . '/../Services/Http/ResponseService.php';

class ResponseProvider implements ServiceProvider
{
    public function register(Container $c): void
    {
        $c->set(ResponseService::class, function () {
            return new ResponseService();
        });
    }
}
