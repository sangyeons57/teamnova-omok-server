package teamnova.omok.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import teamnova.omok.handler.decoder.StringDecoder;
import teamnova.omok.handler.register.FrameHandler;
import teamnova.omok.handler.service.DotenvService;
import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.FramedMessage;
import teamnova.omok.nio.NioReactorServer;

public class AuthHandler implements FrameHandler {
    private static final String EXPECTED_ISSUER = "teamnova-omok";
    private static final String EXPECTED_ALGORITHM = "HS256";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StringDecoder decoder;
    private final String secret;

    public AuthHandler(StringDecoder decoder, DotenvService dotenvService){
        this.decoder = decoder;
        String value = dotenvService.get("JWT_SECRET");
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not configured");
        }
        this.secret = value;
    }

    @Override
    public void handle(NioReactorServer server, ClientSession session, FramedMessage frame) {
        String jwt = decoder.decode(frame.payload());
        if (jwt == null || jwt.isBlank()) {
            session.clearAuthentication();
            System.err.println("JWT payload missing");
            sendResult(server, session, frame, false);
            return;
        }

        try {
            JwtPayload payload = verify(jwt.trim());
            session.markAuthenticated(payload.userId(), payload.role(), payload.scope());
            sendResult(server, session, frame, true);
        } catch (JwtVerificationException e) {
            session.clearAuthentication();
            System.err.println("JWT verification failed: " + e.getMessage());
            sendResult(server, session, frame, false);
        }
    }

    private void sendResult(NioReactorServer server, ClientSession session, FramedMessage frame, boolean success) {
        String message = success ? "1" : "0";
        session.enqueueResponse(frame.type(), frame.requestId(), message.getBytes(StandardCharsets.UTF_8));
        server.enqueueSelectorTask(session::enableWriteInterest);
    }

    private JwtPayload verify(String token) throws JwtVerificationException {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtVerificationException("token must contain header, payload, and signature: " + token);
        }

        JsonNode header = decodeJsonNode(parts[0], "header");
        String algorithm = requiredText(header, "alg");
        if (!EXPECTED_ALGORITHM.equals(algorithm)) {
            throw new JwtVerificationException("unexpected algorithm: " + algorithm);
        }

        JsonNode typeNode = header.get("typ");
        if (typeNode != null && !typeNode.isNull() && !"JWT".equals(typeNode.asText())) {
            throw new JwtVerificationException("unexpected token type: " + typeNode.asText());
        }

        byte[] providedSignature = decodeSignature(parts[2]);
        byte[] expectedSignature = hmac(parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(providedSignature, expectedSignature)) {
            throw new JwtVerificationException("signature verification failed");
        }

        JsonNode claims = decodeJsonNode(parts[1], "payload");
        String issuer = requiredText(claims, "iss");
        if (!EXPECTED_ISSUER.equals(issuer)) {
            throw new JwtVerificationException("unexpected issuer: " + issuer);
        }

        String userId = requiredText(claims, "sub");
        long expiresAt = requiredLong(claims, "exp");
        long now = Instant.now().getEpochSecond();
        if (expiresAt <= now) {
            throw new JwtVerificationException("token expired");
        }

        String role = optionalText(claims, "role");
        String scope = optionalText(claims, "user");

        return new JwtPayload(userId, role, scope);
    }

    private JsonNode decodeJsonNode(String segment, String description) throws JwtVerificationException {
        byte[] decoded;
        try {
            decoded = BASE64_URL_DECODER.decode(segment);
        } catch (IllegalArgumentException e) {
            throw new JwtVerificationException("invalid " + description + " encoding", e);
        }
        try {
            return OBJECT_MAPPER.readTree(decoded);
        } catch (JsonProcessingException e) {
            throw new JwtVerificationException("invalid " + description + " json", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] decodeSignature(String segment) throws JwtVerificationException {
        try {
            return BASE64_URL_DECODER.decode(segment);
        } catch (IllegalArgumentException e) {
            throw new JwtVerificationException("invalid signature encoding", e);
        }
    }

    private byte[] hmac(String data) throws JwtVerificationException {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new JwtVerificationException("failed to sign token", e);
        }
    }

    private static String requiredText(JsonNode node, String field) throws JwtVerificationException {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new JwtVerificationException("missing claim: " + field);
        }
        return value.asText();
    }

    private static String optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private static long requiredLong(JsonNode node, String field) throws JwtVerificationException {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new JwtVerificationException("missing claim: " + field);
        }
        if (value.canConvertToLong()) {
            return value.asLong();
        }
        try {
            return Long.parseLong(value.asText().trim());
        } catch (NumberFormatException e) {
            throw new JwtVerificationException("invalid numeric claim: " + field, e);
        }
    }

    private record JwtPayload(String userId, String role, String scope) { }

    private static final class JwtVerificationException extends Exception {
        JwtVerificationException(String message) {
            super(message);
        }

        JwtVerificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

