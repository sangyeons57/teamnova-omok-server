<?php
class ClockService
{
    /**
     * 현재 UTC 시각 반환
     * @return DateTime
     */
    public function nowUtc(): DateTime
    {
        try {
            return new DateTime('now', new DateTimeZone('UTC'));
        } catch (DateMalformedStringException $e) {
            return new DateTime('now');
        }
    }
}
