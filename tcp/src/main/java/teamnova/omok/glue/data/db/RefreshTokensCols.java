package teamnova.omok.glue.data.db;

public enum RefreshTokensCols {
    TOKEN_HASH("token_hash"),
    USER_ID("user_id"),
    EXPIRES_AT("expires_at"),
    REVOKED_AT("revoekd_at"),
    ISSUED_AT("issued_at");

    public final String col;
    RefreshTokensCols(String col) { this.col = col; }
    @Override public String toString() { return col; }
}
