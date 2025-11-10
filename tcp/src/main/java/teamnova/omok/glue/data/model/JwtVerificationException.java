package teamnova.omok.glue.data.model;

public final class JwtVerificationException extends Exception {
    public JwtVerificationException(String message) {
        super(message);
    }

    public JwtVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
