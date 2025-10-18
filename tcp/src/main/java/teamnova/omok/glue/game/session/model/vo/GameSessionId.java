package teamnova.omok.glue.game.session.model.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * 게임 세션을 식별하는 값 객체.
 */
public record GameSessionId(UUID value) {
    public GameSessionId {
        Objects.requireNonNull(value, "value");
    }

    public static GameSessionId random() {
        return new GameSessionId(UUID.randomUUID());
    }

    public static GameSessionId from(UUID value) {
        return new GameSessionId(value);
    }

    public UUID asUuid() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
