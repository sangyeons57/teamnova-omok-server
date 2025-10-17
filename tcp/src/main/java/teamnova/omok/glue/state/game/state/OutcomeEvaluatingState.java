package teamnova.omok.glue.state.game.state;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import teamnova.omok.glue.game.PlayerResult;
import teamnova.omok.glue.manager.DataManager;
import teamnova.omok.glue.service.ServiceManager;
import teamnova.omok.glue.service.dto.GameCompletionNotice;
import teamnova.omok.glue.service.dto.MoveResult;
import teamnova.omok.glue.state.game.manage.GameSessionStateContext;
import teamnova.omok.glue.state.game.manage.GameSessionStateType;
import teamnova.omok.glue.state.game.manage.TurnCycleContext;
import teamnova.omok.glue.store.GameSession;
import teamnova.omok.glue.store.Stone;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

import javax.swing.plaf.nimbus.State;

/**
 * Determines whether the game concluded after the most recent move.
 */
public final class OutcomeEvaluatingState implements BaseState {
    @Override
    public StateName name() {
        return GameSessionStateType.OUTCOME_EVALUATING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        GameSessionStateContext ctx = (GameSessionStateContext) context;
        TurnCycleContext cycle = ctx.activeTurnCycle();
        GameSession session = ctx.session();

        return (cycle == null) ? noneCycleProcess(ctx, session) : cycleProcess(ctx, session,  cycle);
    }

    private StateStep noneCycleProcess(GameSessionStateContext context, GameSession session) {
        // Unify end conditions: (decided + disconnected == total) OR (connected <= 1)
        if (shouldFinalizeNow(session)) {
            if (connectedCount(session) <= 1) {
                applySoloOrNoneOutcome(session);
            } else {
                applyDisconnectedAsLoss(session);
            }
            finalizeSession(context);
            return StateStep.transition(GameSessionStateType.POST_GAME_DECISION_WAITING.toStateName());
        }
        return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
    }

    private StateStep cycleProcess(GameSessionStateContext context, GameSession session, TurnCycleContext cycle) {
        // 1) Normal win condition (e.g., five-in-a-row) still takes precedence
        boolean finished = handleStonePlaced(cycle.session(), cycle.userId(), cycle.x(), cycle.y(), cycle.stone());
        if (finished) {
            normalWinCondition(cycle, context);
            return StateStep.transition(GameSessionStateType.POST_GAME_DECISION_WAITING.toStateName());
        }

        // 2) Unified finalize path after a non-finishing move
        if (shouldFinalizeNow(session)) {
            if (connectedCount(session) <= 1) {
                applySoloOrNoneOutcome(session);
            } else {
                applyDisconnectedAsLoss(session);
            }
            finalizeSession(context);
            context.clearTurnCycle();
            return StateStep.transition(GameSessionStateType.POST_GAME_DECISION_WAITING.toStateName());
        }

        // 3) Not finished: proceed to finalize the turn
        return StateStep.transition(GameSessionStateType.TURN_FINALIZING.toStateName());
    }

    private void normalWinCondition(TurnCycleContext cycle, GameSessionStateContext context) {
        int turnCount;
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
    }

    private boolean shouldFinalizeNow(GameSession session) {
        if (!session.isGameStarted() || session.isGameFinished()) {
            return false;
        }
        int total = session.getUserIds().size();
        int disconnectedSize = session.disconnectedUsersView().size();
        int connected = total - disconnectedSize;
        if (connected <= 1) {
            return true; // Special termination path
        }
        int decided = (int) session.getUserIds().stream()
                .map(session::outcomeFor)
                .filter(r -> r != null && r != PlayerResult.PENDING)
                .count();

        return decided + disconnectedSize >= total;
    }

    private int connectedCount(GameSession session) {
        List<String> participants = session.getUserIds();
        Set<String> disconnected = session.disconnectedUsersView();
        return participants.size() - disconnected.size();
    }


    private void applyDisconnectedAsLoss(GameSession session) {
        Set<String> disconnected = session.disconnectedUsersView();
        for (String uid : disconnected) {
            PlayerResult r = session.outcomeFor(uid);
            if (r == null || r == PlayerResult.PENDING) {
                session.updateOutcome(uid, PlayerResult.LOSS);
            }
        }
    }

    private void applySoloOrNoneOutcome(GameSession session) {
        List<String> participants = session.getUserIds();
        Set<String> disconnected = session.disconnectedUsersView();
        int connected = participants.size() - disconnected.size();
        if (connected == 1) {
            String winner = null;
            for (String uid : participants) {
                if (!disconnected.contains(uid)) {
                    winner = uid;
                    break;
                }
            }
            if (winner != null) {
                session.updateOutcome(winner, PlayerResult.WIN);
            }
            for (String uid : participants) {
                if (!uid.equals(winner)) {
                    session.updateOutcome(uid, PlayerResult.LOSS);
                }
            }
        } else {
            // No connected players
            for (String uid : participants) {
                session.updateOutcome(uid, PlayerResult.LOSS);
            }
        }
    }

    private void finalizeSession(GameSessionStateContext context) {
        GameSession session = context.session();
        session.lock().lock();
        try {
            int turnCount = session.getTurnStore().getTurnNumber();
            long now = System.currentTimeMillis();
            session.markGameFinished(now, turnCount);
        } finally {
            session.lock().unlock();
        }
        context.pendingGameCompletion(new GameCompletionNotice(session));
    }

    @Override
    public <I extends StateContext> void onExit(I context) {
        GameSessionStateContext ctx = (GameSessionStateContext) context;
        if (ctx.session().isGameFinished()) {
            // Apply scores immediately when the game is confirmed finished
            for(String userId : ctx.session().getUserIds()) {
                int score = ctx.scoreService().calculateScoreDelta(ctx.session(), userId);
                DataManager.getInstance().adjustUserScore(userId, score);
            }
        }
    }

    public boolean handleStonePlaced(GameSession session, String userId, int x, int y, Stone stone) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(stone, "stone");

        if (session.isGameFinished()) {
            return true;
        }

        var boardService = ServiceManager.getInstance().getBoardService();
        if (!boardService.hasFiveInARow(session.getBoardStore(), x, y, stone)) {
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
