<?php
class ValidationService
{
    public function mbLength($s)
    {
        if (function_exists('mb_strlen')) return mb_strlen($s, 'UTF-8');
        return strlen($s);
    }

    public function inArrayStrict($value, $allowed)
    {
        return in_array($value, $allowed, true);
    }
}
