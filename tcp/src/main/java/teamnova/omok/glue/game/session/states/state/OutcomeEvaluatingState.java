package teamnova.omok.glue.game.session.states.state;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.GameScoreService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.messages.GameCompletionNotice;
import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.states.manage.TurnCycleContext;
import teamnova.omok.glue.manager.DataManager;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Determines whether the game concluded after the most recent move.
 */
public final class OutcomeEvaluatingState implements BaseState {
    private final GameBoardService boardService;
    private final GameScoreService scoreService;
    private final GameSessionStateContextService contextService;

    public OutcomeEvaluatingState(GameSessionStateContextService contextService,
                                  GameBoardService boardService,
                                  GameScoreService scoreService) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.boardService = Objects.requireNonNull(boardService, "boardService");
        this.scoreService = Objects.requireNonNull(scoreService, "scoreService");
    }
    @Override
    public StateName name() {
        return GameSessionStateType.OUTCOME_EVALUATING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        GameSessionStateContext ctx = (GameSessionStateContext) context;
        TurnCycleContext cycle = contextService.turn().activeTurnCycle(ctx);
        return (cycle == null) ? noneCycleProcess(ctx) : cycleProcess(ctx, cycle);
    }

    private StateStep noneCycleProcess(GameSessionStateContext context) {
        // Unify end conditions: (decided + disconnected == total) OR (connected <= 1)
        if (shouldFinalizeNow(context)) {
            if (connectedCount(context) <= 1) {
                applySoloOrNoneOutcome(context);
            } else {
                applyDisconnectedAsLoss(context);
            }
            finalizeSession(context);
            return StateStep.transition(GameSessionStateType.POST_GAME_DECISION_WAITING.toStateName());
        }
        return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
    }

    private StateStep cycleProcess(GameSessionStateContext context, TurnCycleContext cycle) {
        // 1) Normal win condition (e.g., five-in-a-row) still takes precedence
        boolean finished = handleStonePlaced(context, cycle.userId(), cycle.x(), cycle.y(), cycle.stone());
        if (finished) {
            normalWinCondition(cycle, context);
            return StateStep.transition(GameSessionStateType.POST_GAME_DECISION_WAITING.toStateName());
        }

        // 2) Unified finalize path after a non-finishing move
        if (shouldFinalizeNow(context)) {
            if (connectedCount(context) <= 1) {
                applySoloOrNoneOutcome(context);
            } else {
                applyDisconnectedAsLoss(context);
            }
            finalizeSession(context);
            contextService.turn().clearTurnCycle(context);
            return StateStep.transition(GameSessionStateType.POST_GAME_DECISION_WAITING.toStateName());
        }

        // 3) Not finished: proceed to finalize the turn
        return StateStep.transition(GameSessionStateType.TURN_PERSONAL_COMPLETED.toStateName());
    }

    private void normalWinCondition(TurnCycleContext cycle, GameSessionStateContext context) {
        int turnCount;
        if (cycle.snapshots().current() != null) {
            turnCount = cycle.snapshots().current().turnNumber();
        } else {
            turnCount = context.turns().actionNumber();
        }
        cycle.session().lock().lock();
        try {
            cycle.session().markGameFinished(cycle.now(), turnCount);
        } finally {
            cycle.session().lock().unlock();
        }
        contextService.turn().queueMoveResult(context, MoveResult.success(
                cycle.session(),
                cycle.stone(),
                null,
                cycle.userId(),
                cycle.x(),
                cycle.y()
        ));
        contextService.postGame().queueGameCompletion(context, new GameCompletionNotice(cycle.session()));
        contextService.turn().clearTurnCycle(context);
    }

    private boolean shouldFinalizeNow(GameSessionStateContext context) {
        if (!context.lifecycle().isGameStarted() || context.outcomes().isGameFinished()) {
            return false;
        }
        int total = context.participants().getUserIds().size();
        int disconnectedSize = context.participants().disconnectedUsersView().size();
        int connected = total - disconnectedSize;
        if (connected <= 1) {
            return true; // Special termination path
        }
        int decided = (int) context.participants().getUserIds().stream()
                .map(context.outcomes()::outcomeFor)
                .filter(r -> r != null && r != PlayerResult.PENDING)
                .count();

        return decided + disconnectedSize >= total;
    }

    private int connectedCount(GameSessionStateContext context) {
        List<String> participants = context.participants().getUserIds();
        Set<String> disconnected = context.participants().disconnectedUsersView();
        return participants.size() - disconnected.size();
    }


    private void applyDisconnectedAsLoss(GameSessionStateContext context) {
        Set<String> disconnected = context.participants().disconnectedUsersView();
        for (String uid : disconnected) {
            PlayerResult r = context.outcomes().outcomeFor(uid);
            if (r == null || r == PlayerResult.PENDING) {
                context.outcomes().updateOutcome(uid, PlayerResult.LOSS);
            }
        }
    }

    private void applySoloOrNoneOutcome(GameSessionStateContext context) {
        List<String> participants = context.participants().getUserIds();
        Set<String> disconnected = context.participants().disconnectedUsersView();
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
                context.outcomes().updateOutcome(winner, PlayerResult.WIN);
            }
            for (String uid : participants) {
                if (!uid.equals(winner)) {
                    context.outcomes().updateOutcome(uid, PlayerResult.LOSS);
                }
            }
        } else {
            // No connected players
            for (String uid : participants) {
                context.outcomes().updateOutcome(uid, PlayerResult.LOSS);
            }
        }
    }

    private void finalizeSession(GameSessionStateContext context) {
        GameSessionAccess session = context.session();
        session.lock().lock();
        try {
            int turnCount = context.turns().actionNumber();
            long now = System.currentTimeMillis();
            context.lifecycle().markGameFinished(now, turnCount);
        } finally {
            session.lock().unlock();
        }
        contextService.postGame().queueGameCompletion(context, new GameCompletionNotice(session));
    }

    @Override
    public <I extends StateContext> void onExit(I context) {
        GameSessionStateContext ctx = (GameSessionStateContext) context;
        GameSessionAccess session = ctx.session();
        if (ctx.outcomes().isGameFinished()) {
            // Apply scores immediately when the game is confirmed finished
            for (String userId : ctx.participants().getUserIds()) {
                int score = scoreService.calculateScoreDelta(session, userId);
                DataManager.getInstance().adjustUserScore(userId, score);
            }
        }
    }

    public boolean handleStonePlaced(GameSessionStateContext context, String userId, int x, int y, Stone stone) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(stone, "stone");

        GameSessionAccess session = context.session();
        if (context.outcomes().isGameFinished()) {
            return true;
        }

        if (!boardService.hasFiveInARow(context.board(), x, y, stone)) {
            return false;
        }

        for (String disconnectedId : context.participants().disconnectedUsersView()) {
            if (!disconnectedId.equals(userId)) {
                context.outcomes().updateOutcome(disconnectedId, PlayerResult.LOSS);
            }
        }
        context.outcomes().updateOutcome(userId, PlayerResult.WIN);
        List<String> userIds = context.participants().getUserIds();
        for (String uid : userIds) {
            if (!uid.equals(userId)) {
                context.outcomes().updateOutcome(uid, PlayerResult.LOSS);
            }
        }
        System.out.printf(
                "[OutcomeService] Game %s finished: winner=%s stone=%s position=(%d,%d)%n",
                session.sessionId().asUuid(),
                userId,
                stone,
                x,
                y
        );
        return true;
    }
}
