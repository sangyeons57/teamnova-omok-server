<?php
require_once __DIR__ . '/Container.php';
require_once __DIR__ . '/ServiceProvider.php';
require_once __DIR__ . '/../Services/Http/ResponseService.php';
require_once __DIR__ . '/../Services/Http/RequestService.php';
require_once __DIR__ . '/../Services/Util/StopwatchService.php';

class HttpServiceProvider implements ServiceProvider
{
    public function register(Container $c): void
    {
        $c->set(ResponseService::class, function (Container $c) {
            return new ResponseService();
        });

        $c->set(RequestService::class, function (Container $c) {
            return new RequestService($c->get(ResponseService::class));
        });
    }
}
