package teamnova.omok.service.db;

public enum AuthProvidersCols {
    USER_ID("user_id"),
    PROVIDER("provider"),
    PROVIDER_USER_ID("provider_user_id"),
    LINKED_AT("linked_at");

    public final String col;
    AuthProvidersCols(String col) { this.col = col; }
    @Override public String toString() { return col; }
}
