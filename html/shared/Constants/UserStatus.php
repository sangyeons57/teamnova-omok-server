<?php
/**
 * 사용자 상태 상수 정의
 * - ACTIVE: 정상
 * - INACTIVE: 비활성(탈퇴/비활동 등)
 * - PENDING: 가입 미완료/승인 대기
 * - BLOCKED: 차단/제재
 */
final class UserStatus
{
    public const ACTIVE   = 'ACTIVE';
    public const INACTIVE = 'INACTIVE';
    public const PENDING  = 'PENDING';
    public const BLOCKED  = 'BLOCKED';

    public static function all(): array
    {
        return array(
            self::ACTIVE,
            self::INACTIVE,
            self::PENDING,
            self::BLOCKED,
        );
    }

    public static function isValid($value): bool
    {
        return in_array($value, self::all(), true);
    }
}
