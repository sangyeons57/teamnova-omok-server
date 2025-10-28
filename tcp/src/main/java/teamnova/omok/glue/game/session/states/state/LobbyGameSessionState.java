package teamnova.omok.glue.game.session.states.state;

import java.util.Map;
import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.services.RuleService;
import teamnova.omok.glue.game.session.states.event.ReadyEvent;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Handles lobby behaviour prior to the game starting.
 */
public class LobbyGameSessionState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final GameSessionServices services;

    public LobbyGameSessionState(GameSessionStateContextService contextService,
                                 GameSessionServices services) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.services = Objects.requireNonNull(services, "services");
    }
    @Override
    public StateName name() {
        return GameSessionStateType.LOBBY.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEvent(I context, BaseEvent event) {
        if (!(context instanceof GameSessionStateContext gameContext)) {
            return StateStep.stay();
        }
        if (event instanceof ReadyEvent readyEvent) {
            return handleReady(gameContext, readyEvent);
        }
        return StateStep.stay();
    }

    private StateStep handleReady(GameSessionStateContext context,
                                  ReadyEvent event) {
        GameSessionAccess session = context.session();
        ReadyResult result;
        session.lock().lock();
        try {
            int playerIndex = context.participants().playerIndexOf(event.userId());
            if (playerIndex < 0) {
                result = ReadyResult.invalid(event.userId(), event.requestId());
            } else {
                boolean changed = context.participants().markReady(event.userId());
                boolean allReady = context.participants().allReady();
                boolean startedNow = false;
                TurnSnapshot snapshot = null;
                if (allReady && !context.lifecycle().isGameStarted()) {
                    startedNow = true;
                    context.lifecycle().markGameStarted(event.timestamp());
                    context.outcomes().resetOutcomes();
                    services.boardService().reset(context.board());
                    snapshot = services.turnService()
                        .start(context.turns(), context.participants().getUserIds(), event.timestamp());
                    snapshot = applyGameStartRules(context, snapshot);
                    contextService.turn().recordTurnSnapshot(
                        context,
                        snapshot,
                        event.timestamp()
                    );
                } else if (context.lifecycle().isGameStarted()) {
                    snapshot = services.turnService().snapshot(context.turns());
                }
                result = new ReadyResult(
                    true,
                    changed,
                    allReady,
                    startedNow,
                    snapshot,
                    event.userId(),
                    event.requestId()
                );
            }
        } finally {
            session.lock().unlock();
        }

        contextService.turn().queueReadyResult(context, result);
        if (!result.validUser()) {
            return StateStep.stay();
        }
        if (result.gameStartedNow()) {
            return StateStep.transition(GameSessionStateType.TURN_START.toStateName());
        }
        return StateStep.stay();
    }

    private TurnSnapshot applyGameStartRules(GameSessionStateContext context, TurnSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        RuleRuntimeContext runtime = new RuleRuntimeContext(
            services,
            contextService,
            context,
            snapshot,
            RuleTriggerKind.GAME_START
        );
        RuleService ruleService = RuleService.getInstance();
        ruleService.setupBoard(context.rules(), runtime);
        Map<String, PlayerResult> seededOutcomes = ruleService.registerParticipantOutcomes(context.rules(), runtime);
        if (!seededOutcomes.isEmpty()) {
            seededOutcomes.forEach((userId, result) -> context.outcomes().updateOutcome(userId, result));
        }
        ruleService.adjustTurnTiming(context.rules(), runtime);
        ruleService.adjustTurnOrder(context.rules(), runtime);
        ruleService.updateTurnBudget(context.rules(), runtime);
        ruleService.activateRules(context.rules(), runtime);
        TurnSnapshot updated = services.turnService().snapshot(context.turns());
        return updated != null ? updated : snapshot;
    }
}
