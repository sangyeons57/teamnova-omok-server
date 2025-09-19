<?php
class StopwatchService
{
    private float $startedAt;

    public function __construct()
    {
        $this->startedAt = microtime(true);
    }

    public function reset(): void
    {
        $this->startedAt = microtime(true);
    }

    public function elapsedMs(): int
    {
        return (int) round((microtime(true) - $this->startedAt) * 1000);
    }
}
