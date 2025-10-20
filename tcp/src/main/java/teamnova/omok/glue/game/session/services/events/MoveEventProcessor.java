package teamnova.omok.glue.game.session.services.events;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.manager.TurnTimeoutScheduler;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.glue.game.session.model.result.MoveStatus;
import teamnova.omok.glue.game.session.services.GameSessionDependencies;
import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.glue.game.session.states.event.MoveEvent;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.handler.register.Type;

/**
 * Processes MOVE events for active sessions.
 */
public final class MoveEventProcessor {

    private final GameSessionDependencies deps;
    private final TimeoutEventProcessor timeoutProcessor;
    private final PostGameEventProcessor postGameProcessor;

    public MoveEventProcessor(GameSessionDependencies deps,
                              TimeoutEventProcessor timeoutProcessor,
                              PostGameEventProcessor postGameProcessor) {
        this.deps = Objects.requireNonNull(deps, "deps");
        this.timeoutProcessor = Objects.requireNonNull(timeoutProcessor, "timeoutProcessor");
        this.postGameProcessor = Objects.requireNonNull(postGameProcessor, "postGameProcessor");
    }

    public void handleCompletion(TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                 GameStateHub manager,
                                 GameSessionStateContext ctx,
                                 MoveEvent event) {
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(event, "event");
        GameSessionStateContextService contextService = deps.contextService();
        MoveResult result = contextService.turn().consumeMoveResult(ctx);
        if (result == null) {
            GameSessionAccess session = manager.session();
            String message;
            if (!session.containsUser(event.userId())) {
                message = "INVALID_PLAYER";
            } else if (!session.isGameStarted()) {
                message = "GAME_NOT_STARTED";
            } else if (session.isGameFinished()) {
                message = "GAME_FINISHED";
            } else {
                message = "TURN_IN_PROGRESS";
            }
            deps.messenger().respondError(event.userId(), Type.PLACE_STONE, event.requestId(), message);
            postGameProcessor.drainSideEffects(ctx, timeoutConsumer);
            return;
        }
        deps.messenger().respondMove(event.userId(), event.requestId(), result);
        if (result.status() == MoveStatus.SUCCESS) {
            deps.messenger().broadcastStonePlaced(result);
            if (result.turnSnapshot() != null && manager.currentType() == GameSessionStateType.TURN_WAITING) {
                timeoutProcessor.scheduleTurnTimeout(result.session(), result.turnSnapshot(), timeoutConsumer);
            } else if (result.turnSnapshot() == null) {
                deps.turnTimeoutScheduler().cancel(result.session().sessionId());
            }
        }
        if (manager.currentType() == GameSessionStateType.COMPLETED) {
            timeoutProcessor.cancelAllTimers(result.session().sessionId());
        }
        postGameProcessor.drainSideEffects(ctx, timeoutConsumer);
    }
}
