package teamnova.omok.glue.game.session.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import teamnova.omok.glue.game.session.interfaces.*;
import teamnova.omok.glue.game.session.interfaces.manager.TurnTimeoutScheduler;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;
import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;

/**
 * GameStateHub 인스턴스를 생성·보관하고 주기 처리까지 담당한다.
 */
public final class GameStateHubRegistry implements GameSessionRuntime {
    private final Map<GameSessionId, GameStateHub> managersById = new ConcurrentHashMap<>();
    private final GameBoardService boardService;
    private final GameTurnService turnService;
    private final GameScoreService scoreService;
    private final GameSessionStateContextService contextService;
    private final GameSessionMessenger messenger;
    private final TurnTimeoutScheduler turnTimeoutScheduler;
    private final DecisionTimeoutScheduler decisionTimeoutScheduler;
    private final GameSessionRepository repository;

    public GameStateHubRegistry(GameSessionRepository repository,
                                GameBoardService boardService,
                                GameTurnService turnService,
                                GameScoreService scoreService,
                                GameSessionStateContextService contextService,
                                GameSessionMessenger messenger,
                                TurnTimeoutScheduler turnTimeoutScheduler,
                                DecisionTimeoutScheduler decisionTimeoutScheduler) {
        this.repository = repository;
        this.boardService = boardService;
        this.turnService = turnService;
        this.scoreService = scoreService;
        this.contextService = contextService;
        this.messenger = messenger;
        this.turnTimeoutScheduler = turnTimeoutScheduler;
        this.decisionTimeoutScheduler = decisionTimeoutScheduler;
    }

    @Override
    public GameStateHub ensure(GameSession session) {
        return managersById.computeIfAbsent(
            session.sessionId(),
            id -> new GameStateHub(session, boardService, turnService, scoreService, contextService, messenger, turnTimeoutScheduler, decisionTimeoutScheduler, repository, this)
        );
    }

    @Override
    public Optional<GameStateHub> find(GameSessionId sessionId) {
        return Optional.ofNullable(managersById.get(sessionId));
    }

    @Override
    public void remove(GameSessionId sessionId) {
        managersById.remove(sessionId);
    }

    @Override
    public void remove(GameSession session) {
        if (session != null) {
            remove(session.sessionId());
        }
    }

    @Override
    public void tick(long now) {
        managersById.values().forEach(manager -> manager.process(now));
    }
}
