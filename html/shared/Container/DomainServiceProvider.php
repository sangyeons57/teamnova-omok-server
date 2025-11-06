<?php
require_once __DIR__ . '/Container.php';
require_once __DIR__ . '/ServiceProvider.php';
require_once __DIR__ . '/../Repositories/UserRepository.php';
require_once __DIR__ . '/../Repositories/AuthProviderRepository.php';
require_once __DIR__ . '/../Services/AccountService.php';
require_once __DIR__ . '/../Services/UserService.php';
require_once __DIR__ . '/../Services/Util/UuidService.php';

class DomainServiceProvider implements ServiceProvider
{
    public function register(Container $c): void
    {
        $c->set(AccountService::class, function (Container $c) {
            return new AccountService(
                $c->get(PDO::class),
                $c->get(UserRepository::class),
                $c->get(AuthProviderRepository::class),
                $c->get(UuidService::class)
            );
        });

        $c->set(UserService::class, function (Container $c) {
            return new UserService(
                $c->get(UserRepository::class),
                $c->get(AuthProviderRepository::class)
            );
        });
    }
}
