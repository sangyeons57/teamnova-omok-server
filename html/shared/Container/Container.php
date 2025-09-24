<?php
/**
 * 간단한 DI 컨테이너
 * - set(id, factory, shared): 서비스 등록
 * - get(id): 지연 생성하여 조회, shared=true면 1회 생성 후 재사용
 */
class Container
{
    /** @var array<string, callable(Container): mixed> */
    private array $definitions = array();

    /** @var array<string, mixed> */
    private array $instances = array();

    /** @var array<string, bool> */
    private array $shared = array();

    public function set(string $id, callable $factory, bool $shared = true): void
    {
        // 동일 ID가 이미 등록되어 있으면 기존 등록을 유지(덮어쓰기 방지)
        if ($this->has($id)) {
            return;
        }
        $this->definitions[$id] = $factory;
        $this->shared[$id] = $shared;
    }

    public function has(string $id): bool
    {
        return array_key_exists($id, $this->instances) || array_key_exists($id, $this->definitions);
    }

    public function get(string $id): mixed
    {
        if (array_key_exists($id, $this->instances)) {
            return $this->instances[$id];
        }
        if (!array_key_exists($id, $this->definitions)) {
            throw new RuntimeException('Service not found: ' . $id);
        }
        $factory = $this->definitions[$id];
        $instance = $factory($this);
        if (($this->shared[$id] ?? true) === true) {
            $this->instances[$id] = $instance;
        }
        return $instance;
    }
}
