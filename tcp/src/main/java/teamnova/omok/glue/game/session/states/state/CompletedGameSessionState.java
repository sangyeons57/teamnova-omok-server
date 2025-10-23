package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.states.event.MoveEvent;
import teamnova.omok.glue.game.session.states.event.PostGameDecisionEvent;
import teamnova.omok.glue.game.session.states.event.ReadyEvent;
import teamnova.omok.glue.game.session.states.event.TimeoutEvent;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionResult;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionStatus;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Terminal state once the session has concluded.
 */
public class CompletedGameSessionState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final GameTurnService turnService;

    public CompletedGameSessionState(GameSessionStateContextService contextService,
                                     GameTurnService turnService) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.turnService = Objects.requireNonNull(turnService, "turnService");
    }
    @Override
    public StateName name() {
        return GameSessionStateType.COMPLETED.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEvent(I context, BaseEvent event) {
        GameSessionStateContext gameContext = (GameSessionStateContext) context;
        if (event instanceof ReadyEvent readyEvent) {
            return handleReady(gameContext, readyEvent);
        }
        if (event instanceof MoveEvent moveEvent) {
            return handleMove(gameContext, moveEvent);
        }
        if (event instanceof TimeoutEvent timeoutEvent) {
            return handleTimeout(gameContext, timeoutEvent);
        }
        if (event instanceof PostGameDecisionEvent decisionEvent) {
            return handlePostGameDecision(gameContext, decisionEvent);
        }
        return StateStep.stay();
    }

    private StateStep handleReady(GameSessionStateContext context,
                                  ReadyEvent event) {
        if (!context.participants().containsUser(event.userId())) {
            contextService.turn().queueReadyResult(context, ReadyResult.invalid(event.userId(), event.requestId()));
            return StateStep.stay();
        }
        boolean allReady = context.participants().allReady();
        TurnSnapshot snapshot =
            turnService.snapshot(context.turns());
        ReadyResult result = new ReadyResult(
            true,
            false,
            allReady,
            false,
            snapshot,
            event.userId(),
            event.requestId()
        );
        contextService.turn().queueReadyResult(context, result);
        return StateStep.stay();
    }

    private StateStep handleMove(GameSessionStateContext context,
                                 MoveEvent event) {
        return StateStep.stay();
    }

    private StateStep handleTimeout(GameSessionStateContext context,
                                    TimeoutEvent event) {
        return StateStep.stay();
    }

    private StateStep handlePostGameDecision(GameSessionStateContext context,
                                             PostGameDecisionEvent event) {
        contextService.postGame().queueDecisionResult(context,
            PostGameDecisionResult.rejected(
                event.userId(),
                PostGameDecisionStatus.SESSION_CLOSED
            )
        );
        return StateStep.stay();
    }
}
