package teamnova.omok.glue.game.session.interfaces;

import java.util.Optional;

import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;

/**
 * 저장 중인 게임 세션 데이터를 관리하는 저장소 계약.
 */
public interface GameSessionRepository {
    GameSession save(GameSession session);

    Optional<GameSession> findById(GameSessionId id);

    Optional<GameSession> findByUserId(String userId);

    Optional<GameSession> removeById(GameSessionId id);

    Optional<GameSession> removeByUserId(String userId);
}
