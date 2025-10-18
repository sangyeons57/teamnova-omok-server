package teamnova.omok.glue.game.session.interfaces;

import java.util.Optional;

import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;

/**
 * 게임 세션에 대응하는 상태 허브를 관리하고 주기적으로 처리한다.
 */
public interface GameSessionRuntime {
    GameStateHub ensure(GameSession session);

    Optional<GameStateHub> find(GameSessionId sessionId);

    void remove(GameSessionId sessionId);

    void remove(GameSession session);

    void tick(long now);
}
