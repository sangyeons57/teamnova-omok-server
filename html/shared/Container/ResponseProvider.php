<?php
require_once __DIR__ . '/../Services/Http/ResponseService.php';
require_once __DIR__ . '/../Services/Util/StopwatchService.php';

class ResponseProvider implements ServiceProvider
{
    public function register(Container $c): void
    {
        $c->set(ResponseService::class, function (Container $c) {
            return new ResponseService($c->get(StopwatchService::class));
        });
    }
}
