package teamnova.omok.glue.data.db;

public enum UserTermsAcceptancesCols {
    USER_ID("user_id"),
    TERMS_ID("terms_id"),
    ACCEPTED_AT("accepted_at");

    public final String col;
    UserTermsAcceptancesCols(String col) { this.col = col; }
    @Override public String toString() { return col; }
}
