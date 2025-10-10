package teamnova.omok.state.game.state;

import teamnova.omok.service.InGameSessionService;
import teamnova.omok.service.TurnService;
import teamnova.omok.state.game.contract.GameSessionState;
import teamnova.omok.state.game.event.GameSessionEventRegistry;
import teamnova.omok.state.game.event.GameSessionEventType;
import teamnova.omok.state.game.event.ReadyEvent;
import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.state.game.manage.GameSessionStateStep;
import teamnova.omok.state.game.manage.GameSessionStateType;
import teamnova.omok.store.GameSession;

/**
 * Handles lobby behaviour prior to the game starting.
 */
public class LobbyGameSessionState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.LOBBY;
    }

    @Override
    public void registerHandlers(GameSessionEventRegistry registry) {
        registry.register(GameSessionEventType.READY, ReadyEvent.class, this::handleReady);
    }

    private GameSessionStateStep handleReady(GameSessionStateContext context,
                                             ReadyEvent event) {
        GameSession session = context.session();
        InGameSessionService.ReadyResult result;
        session.lock().lock();
        try {
            int playerIndex = session.playerIndexOf(event.userId());
            if (playerIndex < 0) {
                result = InGameSessionService.ReadyResult.invalid(session, event.userId());
            } else {
                boolean changed = session.markReady(event.userId());
                boolean allReady = session.allReady();
                boolean startedNow = false;
                TurnService.TurnSnapshot snapshot = null;
                if (allReady && !session.isGameStarted()) {
                    startedNow = true;
                    session.markGameStarted(event.timestamp());
                    session.resetOutcomes();
                    context.boardService().reset(session.getBoardStore());
                    snapshot = context.turnService()
                        .start(session.getTurnStore(), session.getUserIds(), event.timestamp());
                } else if (session.isGameStarted()) {
                    snapshot = context.turnService()
                        .snapshot(session.getTurnStore(), session.getUserIds());
                }
                result = new InGameSessionService.ReadyResult(
                    session,
                    true,
                    changed,
                    allReady,
                    startedNow,
                    snapshot,
                    event.userId()
                );
            }
        } finally {
            session.lock().unlock();
        }

        context.pendingReadyResult(result);
        if (!result.validUser()) {
            return GameSessionStateStep.stay();
        }
        if (result.gameStartedNow()) {
            return GameSessionStateStep.transition(GameSessionStateType.TURN_WAITING);
        }
        return GameSessionStateStep.stay();
    }
}
