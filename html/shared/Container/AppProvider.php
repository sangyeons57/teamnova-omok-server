<?php
require_once __DIR__ . '/../Repositories/TermsRepository.php';
require_once __DIR__ . '/../Services/Util/NormalizeService.php';
require_once __DIR__ . '/../Services/Util/UuidService.php';
require_once __DIR__ . '/../Services/UserService.php';
class AppProvider implements ServiceProvider
{
    public function register(Container $c): void
    {
        // PDO (Database 헬퍼를 통해 획득)
        $c->set(PDO::class, function () {
            return Database::pdo();
        });

        // Repositories
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

        // Services
        $c->set(AccountService::class, function (Container $c) {
            return new AccountService(
                $c->get(PDO::class),
                $c->get(UserRepository::class),
                $c->get(AuthProviderRepository::class),
                $c->get(UuidService::class)
            );
        });
        $c->set(UserService::class, function (Container $c) {
            return new UserService($c->get(UserRepository::class));
        });
    }
}
