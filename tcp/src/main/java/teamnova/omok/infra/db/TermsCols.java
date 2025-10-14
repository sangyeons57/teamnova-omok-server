package teamnova.omok.infra.db;

public enum TermsCols {
    TERMS_ID("terms_id"),
    TERMS_TYPE("terms_type"),
    VERSION("version"),
    IS_REQUIRED("is_required"),
    PUBLISHED_AT("published_at"),
    CREATED_AT("created_at");

    public final String col;
    TermsCols(String col) { this.col = col; }
    @Override public String toString() { return col; }
}
