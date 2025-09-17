<?php
final class Normalize
{
    /**
     * 문자열 배열 정규화:
     * - 문자열이 아닌 값은 무시
     * - trim 후 빈 문자열은 제거
     * - 입력 순서를 유지하며 중복 제거
     */
    public static function stringArrayDistinctTrimmed(array $values): array
    {
        $out = array();
        $seen = array();
        foreach ($values as $v) {
            if (!is_string($v)) {
                continue;
            }
            $v = trim($v);
            if ($v === '') {
                continue;
            }
            if (isset($seen[$v])) {
                continue;
            }
            $seen[$v] = true;
            $out[] = $v;
        }
        return $out;
    }
}
