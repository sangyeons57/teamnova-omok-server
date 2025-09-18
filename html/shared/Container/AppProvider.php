<?php
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

        // Services
        $c->set(AccountService::class, function (Container $c) {
            return new AccountService($c->get(PDO::class));
        });

        $c->set(TokenService::class, function (Container $c) {
            return new TokenService(
                $c->get(RefreshTokenRepository::class),
                $c->get(UserRepository::class)
            );
        });
    }
}
