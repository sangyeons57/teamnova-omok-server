<?php
class UuidService
{
    /**
     * UUID v4 생성
     * @return string
     */
    public function v4(): string
    {
        $data = function_exists('random_bytes') ? random_bytes(16) : $this->fallbackRandom(16);

        // version(4)과 variant(10x) 비트 설정
        $data[6] = chr((ord($data[6]) & 0x0f) | 0x40);
        $data[8] = chr((ord($data[8]) & 0x3f) | 0x80);

        $hex = bin2hex($data);
        return sprintf(
            '%s-%s-%s-%s-%s',
            substr($hex, 0, 8),
            substr($hex, 8, 4),
            substr($hex, 12, 4),
            substr($hex, 16, 4),
            substr($hex, 20, 12)
        );
    }

    /**
     * random_bytes 미지원 환경을 위한 폴백
     * @param int $length
     * @return string
     */
    private function fallbackRandom(int $length): string
    {
        if (function_exists('openssl_random_pseudo_bytes')) {
            $strong = false;
            $bytes = openssl_random_pseudo_bytes($length, $strong);
            if ($bytes) {
                return $bytes;
            }
        }
        $bytes = '';
        for ($i = 0; $i < $length; $i++) {
            $bytes .= chr(mt_rand(0, 255));
        }
        return $bytes;
    }
}
