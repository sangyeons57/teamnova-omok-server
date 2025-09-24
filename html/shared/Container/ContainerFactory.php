<?php
require_once __DIR__ . '/Container.php';
require_once __DIR__ . '/ServiceProvider.php';
require_once __DIR__ . '/InfrastructureProvider.php';
require_once __DIR__ . '/DomainServiceProvider.php';
require_once __DIR__ . '/AuthServiceProvider.php';
require_once __DIR__ . '/HttpServiceProvider.php';
require_once __DIR__ . '/SecurityProvider.php';
require_once __DIR__ . '/UtilProvider.php';

class ContainerFactory
{
    /**
     * 생성된 컨테이너에 기본 프로바이더들을 등록합니다.
     * @param ServiceProvider[] $extraProviders 추가 등록이 필요한 경우 전달
     */
    public static function create(array $extraProviders = array()): Container
    {
        $container = new Container();

        $providers = array_merge(
            array(
                new UtilProvider(),
                new InfrastructureProvider(),
                new DomainServiceProvider(),
                new AuthServiceProvider(),
                new HttpServiceProvider(),
                new SecurityProvider(),
            ),
            $extraProviders
        );

        foreach ($providers as $provider) {
            if ($provider instanceof ServiceProvider) {
                $provider->register($container);
            } else {
                throw new InvalidArgumentException('All providers must implement ServiceProvider');
            }
        }

        return $container;
    }
}
