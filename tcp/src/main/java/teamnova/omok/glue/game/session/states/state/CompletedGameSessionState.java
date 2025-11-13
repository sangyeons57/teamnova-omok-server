package teamnova.omok.glue.game.session.states.state;

import java.util.List;
import java.util.Objects;

import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.states.event.MoveEvent;
import teamnova.omok.glue.game.session.states.event.PostGameDecisionEvent;
import teamnova.omok.glue.game.session.states.event.ReadyEvent;
import teamnova.omok.glue.game.session.states.event.TimeoutEvent;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
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
    private final GameSessionServices services;
    private final GameTurnService turnService;

    public CompletedGameSessionState(GameSessionStateContextService contextService,
                                     GameSessionServices services) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.services = Objects.requireNonNull(services, "services");
        this.turnService = services.turnService();
    }
    @Override
    public StateName name() {
        return GameSessionStateType.COMPLETED.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        GameSessionStateContext ctx = (GameSessionStateContext) context;
        System.out.println("[SESSION][" + ctx.session().sessionId() + "] Game session completed");
        cleanup(ctx);
        return StateStep.stay();
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
        return StateStep.stay();
    }

    private void cleanup(GameSessionStateContext context) {
        var session = context.session();
        var sessionId = session.sessionId();

        // Stop outstanding timers before clearing repository/runtime
        services.turnTimeoutScheduler().cancel(sessionId);
        services.decisionTimeoutScheduler().cancel(sessionId);

        List<String> userIds = List.copyOf(session.getUserIds());
        session.lock().lock();
        try {
            for (String userId : userIds) {
                session.markDisconnected(userId);
            }
        } finally {
            session.lock().unlock();
        }

        services.repository().removeById(sessionId);
        services.runtime().remove(sessionId);

        for (String userId : userIds) {
            ClientSessionManager.getInstance()
                .findSession(userId)
                .ifPresent(handle -> {
                    var currentSessionId = handle.currentGameSessionId();
                    if (sessionId.equals(currentSessionId)) {
                        handle.unbindGameSession(sessionId);
                        handle.exitGameSession();
                    }
                });
        }
    }
}
