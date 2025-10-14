package teamnova.omok.domain.session.game.entity.state.state;

import teamnova.omok.service.dto.MoveResult;
import teamnova.omok.service.dto.MoveStatus;
import teamnova.omok.service.dto.PostGameDecisionResult;
import teamnova.omok.service.dto.PostGameDecisionStatus;
import teamnova.omok.service.dto.ReadyResult;
import teamnova.omok.service.dto.TurnTimeoutResult;
import teamnova.omok.domain.session.game.entity.turn.TurnSnapshot;
import teamnova.omok.domain.session.game.entity.state.contract.GameSessionEvent;
import teamnova.omok.domain.session.game.entity.state.contract.GameSessionState;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateContext;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateStep;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateType;
import teamnova.omok.domain.session.game.GameSession;

/**
 * Terminal state once the session has concluded.
 */
public class CompletedGameSessionState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.COMPLETED;
    }

    @Override
    public GameSessionStateStep onEvent(GameSessionStateContext context, GameSessionEvent event) {
        return switch (event) {
            case GameSessionEvent.Ready ignored ->  handleReady(context, (GameSessionEvent.Ready) event);
            case GameSessionEvent.Move ignored -> handleMove(context, (GameSessionEvent.Move) event);
            case GameSessionEvent.Timeout ignored -> handleTimeout(context, (GameSessionEvent.Timeout) event);
            case GameSessionEvent.PostGameDecision ignored -> handlePostGameDecision(context, (GameSessionEvent.PostGameDecision) event);

            default -> GameSessionStateStep.stay();
        };
    }

    private GameSessionStateStep handleReady(GameSessionStateContext context,
                                             GameSessionEvent.Ready event) {
        GameSession session = context.session();
        if (!session.containsUser(event.userId())) {
            context.pendingReadyResult(ReadyResult.invalid(session, event.userId()));
            return GameSessionStateStep.stay();
        }
        boolean allReady = session.allReady();
        TurnSnapshot snapshot = session.snapshotTurn();
        ReadyResult result = new ReadyResult(
            session,
            true,
            false,
            allReady,
            false,
            snapshot,
            event.userId()
        );
        context.pendingReadyResult(result);
        return GameSessionStateStep.stay();
    }

    private GameSessionStateStep handleMove(GameSessionStateContext context,
                                            GameSessionEvent.Move event) {
        GameSession session = context.session();
        if (session.playerIndexOf(event.userId()) < 0) {
            context.pendingMoveResult(
                MoveResult.invalid(
                    session,
                    MoveStatus.INVALID_PLAYER,
                    null,
                    event.userId(),
                    event.x(),
                    event.y()
                )
            );
            return GameSessionStateStep.stay();
        }
        TurnSnapshot snapshot = session.snapshotTurn();
        context.pendingMoveResult(MoveResult.invalid(
            session,
            MoveStatus.GAME_FINISHED,
            snapshot,
            event.userId(),
            event.x(),
            event.y()
        ));
        return GameSessionStateStep.stay();
    }

    private GameSessionStateStep handleTimeout(GameSessionStateContext context,
                                               GameSessionEvent.Timeout event) {
        GameSession session = context.session();
        TurnSnapshot snapshot = session.snapshotTurn();
        context.pendingTimeoutResult(
            TurnTimeoutResult.noop(session, snapshot)
        );
        return GameSessionStateStep.stay();
    }

    private GameSessionStateStep handlePostGameDecision(GameSessionStateContext context,
                                                        GameSessionEvent.PostGameDecision event) {
        GameSession session = context.session();
        context.pendingDecisionResult(
            PostGameDecisionResult.rejected(
                session,
                event.userId(),
                PostGameDecisionStatus.SESSION_CLOSED
            )
        );
        return GameSessionStateStep.stay();
    }
}
