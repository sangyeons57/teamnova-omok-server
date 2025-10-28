package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.log.GameSessionLogger;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.services.RuleService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Writes the validated stone onto the board.
 */
public final class MoveApplyingState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final GameSessionServices services;

    public MoveApplyingState(GameSessionStateContextService contextService,
                             GameSessionServices services) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.services = Objects.requireNonNull(services, "services");
    }
    @Override
    public StateName name() {
        return GameSessionStateType.MOVE_APPLYING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        return onEnterInternal((GameSessionStateContext) context);
    }

    private StateStep onEnterInternal(GameSessionStateContext context) {
        TurnPersonalFrame frame = contextService.turn().currentPersonalTurn(context);
        if (frame == null || !frame.hasActiveMove()) {
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }
        fireRules(context, RuleTriggerKind.PRE_PLACEMENT);

        int x = frame.x();
        int y = frame.y();
        if (!services.boardService().isWithinBounds(context.board(), x, y)
            || !services.boardService().isEmpty(context.board(), x, y)) {
            // fallback to original validated location if rule chose an invalid spot
            frame.updatePosition(frame.originalX(), frame.originalY());
            x = frame.x();
            y = frame.y();
        }

        int playerIndex = context.participants().playerIndexOf(frame.userId());
        StonePlacementMetadata metadata;
        if (frame.currentSnapshot() != null) {
            metadata = StonePlacementMetadata.forPlayer(
                frame.currentSnapshot(),
                frame.userId(),
                playerIndex
            );
        } else {
            metadata = StonePlacementMetadata.empty();
        }
        services.boardService().setStone(context.board(), x, y, frame.stone(), metadata);
        GameSessionLogger.event(context, GameSessionStateType.MOVE_APPLYING, "StonePlaced",
            String.format("user=%s x=%d y=%d stone=%s", frame.userId(), x, y, frame.stone()));
        fireRules(context, RuleTriggerKind.POST_PLACEMENT);
        return StateStep.transition(GameSessionStateType.TURN_PERSONAL_END.toStateName());
    }

    private void fireRules(GameSessionStateContext context, RuleTriggerKind triggerKind) {
        TurnSnapshot snapshot = services.turnService().snapshot(context.turns());
        RuleRuntimeContext runtime = new RuleRuntimeContext(
            services,
            contextService,
            context,
            snapshot,
            triggerKind
        );
        RuleService ruleService = RuleService.getInstance();
        if (triggerKind == RuleTriggerKind.PRE_PLACEMENT) {
            ruleService.applyMoveMutations(context.rules(), runtime);
            ruleService.queueHiddenPlacement(context.rules(), runtime);
        }
        ruleService.activateRules(context.rules(), runtime);
        if (triggerKind == RuleTriggerKind.POST_PLACEMENT) {
            ruleService.revealHiddenPlacements(context.rules(), runtime);
        }
    }
}
