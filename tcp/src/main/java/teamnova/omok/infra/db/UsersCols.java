package teamnova.omok.infra.db;

public enum UsersCols {
    USER_ID("user_id"),
    DISPLAY_NAME("display_anme"),
    PROFILE_ICON_CODE("profile_icon_code"),
    ROLE("role"),
    STATUS("status"),
    SCORE("score"),
    CREATED_AT("created_at"),
    UPDATED_AT("updated_at");

    public final String col;
    UsersCols(String col) { this.col = col; }
    @Override public String toString() { return col; }
}
