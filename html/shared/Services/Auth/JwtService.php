<?php
class JwtService {
    private static function base64url($data) {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }

    public static function encodeHS256($payload, $secret) {
        $header = array('alg' => 'HS256', 'typ' => 'JWT');
        $segments = array(
            self::base64url(json_encode($header)),
            self::base64url(json_encode($payload))
        );
        $signingInput = implode('.', $segments);
        $signature = hash_hmac('sha256', $signingInput, $secret, true);
        $segments[] = self::base64url($signature);
        return implode('.', $segments);
    }
}
