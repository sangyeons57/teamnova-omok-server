package teamnova.omok.domain.session.game.entity.state.state;

import teamnova.omok.service.dto.ReadyResult;
import teamnova.omok.domain.session.game.entity.turn.TurnSnapshot;
import teamnova.omok.domain.session.game.entity.state.contract.GameSessionEvent;
import teamnova.omok.domain.session.game.entity.state.contract.GameSessionState;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateContext;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateStep;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateType;
import teamnova.omok.domain.session.game.GameSession;

/**
 * Handles lobby behaviour prior to the game starting.
 */
public class LobbyGameSessionState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.LOBBY;
    }


    @Override
    public GameSessionStateStep onEvent(GameSessionStateContext context, GameSessionEvent event) {
        return switch (event) {
            case GameSessionEvent.Ready ignored ->  handleReady(context, (GameSessionEvent.Ready) event);
            default -> GameSessionStateStep.stay();
        };
    }

    private GameSessionStateStep handleReady(GameSessionStateContext context,
                                             GameSessionEvent.Ready event) {
        GameSession session = context.session();
        ReadyResult result;
        session.lock().lock();
        try {
            int playerIndex = session.playerIndexOf(event.userId());
            if (playerIndex < 0) {
                result = ReadyResult.invalid(session, event.userId());
            } else {
                boolean changed = session.markReady(event.userId());
                boolean allReady = session.allReady();
                boolean startedNow = false;
                TurnSnapshot snapshot = null;
                if (allReady && !session.isGameStarted()) {
                    startedNow = true;
                    session.markGameStarted(event.timestamp());
                    session.resetOutcomes();
                    session.reset();
                    snapshot = session.startTurn(event.timestamp());
                } else if (session.isGameStarted()) {
                    snapshot = session.snapshotTurn();
                }
                result = new ReadyResult(
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
