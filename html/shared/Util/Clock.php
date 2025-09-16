<?php
class Clock {
    public static function nowUtc() {
        return new DateTime('now', new DateTimeZone('UTC'));
    }
}
