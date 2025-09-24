<?php
require_once __DIR__ . '/Container.php';
require_once __DIR__ . '/ServiceProvider.php';
require_once __DIR__ . '/../Database.php';
require_once __DIR__ . '/../Repositories/UserRepository.php';
require_once __DIR__ . '/../Repositories/RefreshTokenRepository.php';
require_once __DIR__ . '/../Repositories/AuthProviderRepository.php';
require_once __DIR__ . '/../Repositories/TermsRepository.php';
require_once __DIR__ . '/../Services/Util/NormalizeService.php';

class InfrastructureProvider implements ServiceProvider
{
    public function register(Container $c): void
    {
        $c->set(PDO::class, function () {
            return Database::pdo();
        });

        $c->set(UserRepository::class, function (Container $c) {
            return new UserRepository($c->get(PDO::class));
        });

        $c->set(RefreshTokenRepository::class, function (Container $c) {
            return new RefreshTokenRepository($c->get(PDO::class));
        });

        $c->set(AuthProviderRepository::class, function (Container $c) {
            return new AuthProviderRepository($c->get(PDO::class));
        });

        $c->set(TermsRepository::class, function (Container $c) {
            return new TermsRepository(
                $c->get(PDO::class),
                $c->get(NormalizeService::class)
            );
        });
    }
}
