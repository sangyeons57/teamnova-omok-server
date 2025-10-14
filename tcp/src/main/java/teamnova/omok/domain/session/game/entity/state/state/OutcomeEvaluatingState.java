package teamnova.omok.domain.session.game.entity.state.state;

import teamnova.omok.game.PlayerResult;
import teamnova.omok.service.dto.GameCompletionNotice;
import teamnova.omok.service.dto.MoveResult;
import teamnova.omok.domain.session.game.GameSession;
import teamnova.omok.domain.session.game.entity.stone.Stone;
import teamnova.omok.domain.session.game.entity.state.contract.GameSessionState;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateContext;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateStep;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateType;
import teamnova.omok.domain.session.game.entity.state.manage.TurnCycleContext;

import java.util.List;
import java.util.Objects;

/**
 * Determines whether the game concluded after the most recent move.
 */
public final class OutcomeEvaluatingState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.OUTCOME_EVALUATING;
    }

    @Override
    public GameSessionStateStep onEnter(GameSessionStateContext context) {
        TurnCycleContext cycle = context.activeTurnCycle();
        if (cycle == null) {
            return GameSessionStateStep.transition(GameSessionStateType.TURN_WAITING);
        }
        boolean finished = handleStonePlaced(cycle.session(), cycle.userId(), cycle.x(), cycle.y(), cycle.stone());
        if (finished) {
            int turnCount = 0;
            if (cycle.snapshots().current() != null) {
                turnCount = cycle.snapshots().current().turnNumber();
            } else {
                turnCount = cycle.session().getTurnStore().getTurnNumber();
            }
            cycle.session().lock().lock();
            try {
                cycle.session().markGameFinished(cycle.now(), turnCount);
            } finally {
                cycle.session().lock().unlock();
            }
            context.pendingMoveResult(MoveResult.success(
                cycle.session(),
                cycle.stone(),
                null,
                cycle.userId(),
                cycle.x(),
                cycle.y()
            ));
            context.pendingGameCompletion(new GameCompletionNotice(cycle.session()));
            context.clearTurnCycle();
            return GameSessionStateStep.transition(GameSessionStateType.POST_GAME_DECISION_WAITING);
        }
        return GameSessionStateStep.transition(GameSessionStateType.TURN_FINALIZING);
    }

    private boolean handleStonePlaced(GameSession session, String userId, int x, int y, Stone stone) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(stone, "stone");

        if (session.isGameFinished()) {
            return true;
        }

        if (!session.getBoard().hasFiveInARow(x, y, stone)) {
            return false;
        }

        for (String disconnectedId : session.disconnectedUsersView()) {
            if (!disconnectedId.equals(userId)) {
                session.updateOutcome(disconnectedId, PlayerResult.LOSS);
            }
        }
        session.updateOutcome(userId, PlayerResult.WIN);
        List<String> userIds = session.getUserIds();
        for (String uid : userIds) {
            if (!uid.equals(userId)) {
                session.updateOutcome(uid, PlayerResult.LOSS);
            }
        }
        System.out.printf(
                "[OutcomeService] Game %s finished: winner=%s stone=%s position=(%d,%d)%n",
                session.getId(),
                userId,
                stone,
                x,
                y
        );
        return true;
    }
}
