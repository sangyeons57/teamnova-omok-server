package teamnova.omok.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Simple MySQL service that reads configuration from DotenvService
 * and exposes connection properties/JDBC URL. Also provides minimal
 * helper queries used by the server.
 */
public class MysqlService {
    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;

    public MysqlService(DotenvService dotenv) {
        this.host = orDefault(dotenv.get("DB_HOST"), "127.0.0.1");
        this.port = parseInt(orDefault(dotenv.get("DB_PORT"), "3306"), 3306);
        this.database = orDefault(dotenv.get("DB_NAME"), "");
        this.user = orDefault(dotenv.get("DB_USER"), "");
        this.password = orDefault(dotenv.get("DB_PASS"), "");
    }

    public String jdbcUrl() {
        String db = (database == null || database.isBlank()) ? "" : "/" + database;
        return "jdbc:mysql://" + host + ":" + port + db + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    /**
     * Fetches a user's score from the users table using their user_id.
     * Returns defaultScore if not found or if any error occurs.
     */
    public int getUserScore(String userId, int defaultScore) {
        if (userId == null || userId.isBlank()) return defaultScore;
        // If configuration is incomplete, bail out quickly
        if (user == null || user.isBlank()) return defaultScore;
        String url = jdbcUrl();
        String sql = "SELECT score FROM users WHERE user_id = ? LIMIT 1";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int score = rs.getInt(1);
                    return rs.wasNull() ? defaultScore : score;
                }
            }
        } catch (SQLException e) {
            // Log quietly to stderr; return default on failure
            System.err.println("[MysqlService] getUserScore failed: " + e.getMessage());
        }
        return defaultScore;
    }

    /**
     * Adjusts a user's score by the provided delta. Returns true if at least one row was updated.
     */
    public boolean adjustUserScore(String userId, int delta) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        if (delta == 0) {
            return true;
        }
        if (user == null || user.isBlank()) {
            return false;
        }
        String url = jdbcUrl();
        String sql = "UPDATE users SET score = score + ? WHERE user_id = ?";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setString(2, userId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                System.err.printf("[MysqlService] adjustUserScore: no row updated for user %s%n", userId);
            }
            return updated > 0;
        } catch (SQLException e) {
            System.err.printf("[MysqlService] adjustUserScore failed for %s: %s%n", userId, e.getMessage());
            if (isScoreCheckViolation(e) && resetUserScoreToZero(userId)) {
                System.err.printf(
                    "[MysqlService] adjustUserScore fallback: reset score to zero for user %s after failure%n",
                    userId
                );
                return true;
            }
            return false;
        }
    }

    private boolean isScoreCheckViolation(SQLException e) {
        if (e == null) {
            return false;
        }
        if (e.getErrorCode() == 3819) {
            return true;
        }
        String message = e.getMessage();
        return message != null && message.contains("ScoreCheck");
    }

    private boolean resetUserScoreToZero(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        if (user == null || user.isBlank()) {
            return false;
        }
        String url = jdbcUrl();
        String sql = "UPDATE users SET score = 0 WHERE user_id = ?";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.printf("[MysqlService] resetUserScoreToZero failed for %s: %s%n", userId, ex.getMessage());
            return false;
        }
    }

    private static String orDefault(String v, String d) { return (v == null || v.isBlank()) ? d : v; }
    private static int parseInt(String v, int d) {
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return d; }
    }
}
