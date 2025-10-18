package teamnova.omok.glue.data.db;

public enum Tables {
    AUTH_PROVIDERS("auth_providers"),
    REFRESH_TOKENS("refresh_tokens"),
    TERMS("terms"),
    USER_TERMS_ACCEPTANCES("user_terms_acceptances"),
    USERS("users");

    public final String table;
    Tables(String table) { this.table = table; }
    @Override public String toString() { return table; }
}
