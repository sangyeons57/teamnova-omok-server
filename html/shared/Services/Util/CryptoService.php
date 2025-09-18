<?php

use Random\RandomException;

class CryptoService
{
    /**
     * 암호학적 난수 바이트
     * @param int $length
     * @return string
     * @throws RandomException
     */
    public function randomBytes($length): string
    {
        if (function_exists('random_bytes')) {
            return random_bytes($length);
        }
        if (function_exists('openssl_random_pseudo_bytes')) {
            $strong = false;
            $bytes = openssl_random_pseudo_bytes($length, $strong);
            if ($bytes && $strong) {
                return $bytes;
            }
        }
        $bytes = '';
        for ($i = 0; $i < $length; $i++) {
            $bytes .= chr(mt_rand(0, 255));
        }
        return $bytes;
    }

    /**
     * Base64 URL-safe 인코딩
     * @param string $data
     * @return string
     */
    public function base64url($data): string
    {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }

    /**
     * SHA-256 해시(hex)
     * @param string $data
     * @return string
     */
    public function sha256($data): string
    {
        return hash('sha256', $data);
    }
}
