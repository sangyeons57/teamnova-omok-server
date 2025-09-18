<?php
interface ServiceProvider
{
    public function register(Container $c): void;
}
