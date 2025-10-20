package teamnova.omok.glue.game.session.services.events;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.manager.TurnTimeoutScheduler;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.services.GameSessionDependencies;
import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.glue.game.session.states.event.ReadyEvent;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.handler.register.Type;

/**
 * Handles READY events emitted by clients during a session.
 */
public final class ReadyEventProcessor {

    private final GameSessionDependencies deps;
    private final TimeoutEventProcessor timeoutProcessor;
    private final PostGameEventProcessor postGameProcessor;

    public ReadyEventProcessor(GameSessionDependencies deps,
                               TimeoutEventProcessor timeoutProcessor,
                               PostGameEventProcessor postGameProcessor) {
        this.deps = Objects.requireNonNull(deps, "deps");
        this.timeoutProcessor = Objects.requireNonNull(timeoutProcessor, "timeoutProcessor");
        this.postGameProcessor = Objects.requireNonNull(postGameProcessor, "postGameProcessor");
    }

    public void handleCompletion(TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                 GameStateHub manager,
                                 GameSessionStateContext ctx,
                                 ReadyEvent event) {
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(event, "event");
        GameSessionStateContextService contextService = deps.contextService();
        ReadyResult result = contextService.turn().consumeReadyResult(ctx);
        if (result == null) {
            GameSessionAccess session = manager.session();
            String message;
            if (!session.containsUser(event.userId())) {
                message = "INVALID_PLAYER";
            } else if (manager.currentType() == GameSessionStateType.COMPLETED) {
                message = "GAME_FINISHED";
            } else if (!session.isGameStarted()) {
                message = "GAME_NOT_STARTED";
            } else {
                message = "INVALID_STATE";
            }
            deps.messenger().respondError(event.userId(), Type.READY_IN_GAME_SESSION, event.requestId(), message);
            postGameProcessor.drainSideEffects(ctx, timeoutConsumer);
            return;
        }
        if (!result.validUser()) {
            deps.messenger().respondError(event.userId(), Type.READY_IN_GAME_SESSION, event.requestId(), "INVALID_PLAYER");
            postGameProcessor.drainSideEffects(ctx, timeoutConsumer);
            return;
        }
        deps.messenger().respondReady(event.userId(), event.requestId(), result);
        if (result.stateChanged()) {
            deps.messenger().broadcastReady(result);
        }
        if (result.gameStartedNow() && result.firstTurn() != null) {
            deps.messenger().broadcastGameStart(result.session(), result.firstTurn());
            timeoutProcessor.scheduleTurnTimeout(result.session(), result.firstTurn(), timeoutConsumer);
        }
        if (manager.currentType() == GameSessionStateType.COMPLETED) {
            timeoutProcessor.cancelAllTimers(result.session().sessionId());
        }
        postGameProcessor.drainSideEffects(ctx, timeoutConsumer);
    }
}
