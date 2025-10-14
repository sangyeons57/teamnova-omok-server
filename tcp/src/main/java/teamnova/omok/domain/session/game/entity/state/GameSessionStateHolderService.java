package teamnova.omok.domain.session.game.entity.state;

import teamnova.omok.application.register.GameSessionStateRegister;
import teamnova.omok.domain.rule.RulesContext;
import teamnova.omok.domain.session.game.entity.state.contract.GameSessionEvent;
import teamnova.omok.domain.session.game.entity.state.contract.GameSessionState;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateStep;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateType;

import java.util.Objects;

public class GameSessionStateHolderService {

    public void process(GameSessionStateHolder holder, GameSessionStateRegister register, long now) {
        GameSessionStateHolder.PendingEvent pending;
        GameSessionState state = register.get(holder.getType());
        while ((pending = holder.poll()) != null) {
            processPending(holder, pending);
        }
        GameSessionStateStep updateStep = state.onUpdate(context, now);
        applyTransition(updateStep);
    }

    private void processPending(GameSessionState state, GameSessionStateHolder.PendingEvent pending) {
        handleEvent(state, pending.event());
        if (pending.callback() != null) {
            pending.callback().accept(context);
        }
    }

    private GameSessionStateStep handleEvent(GameSessionState state, GameSessionEvent event) {
        Objects.requireNonNull(event, "event");
        GameSessionStateStep step = state.onEvent(context, event);
        applyTransition(step);
        return step;
    }

    private void applyTransition(GameSessionStateStep step) {
        if (step != null && step.hasTransition()) {
            transitionDirect(step.nextState());
        }
    }

    private void transitionDirect(GameSessionStateType targetType) {
        GameSessionState next = registry.get(targetType);
        if (next == null) {
            throw new IllegalStateException("No state registered for type " + targetType);
        }
        if (next == currentState) {
            return;
        }

        next.onExit(context);
        currentState = next;
        triggerRules(targetType);
        applyTransition(currentState.onEnter(context));
    }

    private void triggerRules(GameSessionStateType targetType) {
        RulesContext rulesContext = context.session().getRulesContext();
        if (rulesContext == null) {
            return;
        }
        rulesContext.attachStateContext(context);
        if (rulesContext.setCurrentRuleByGameState(targetType)) {
            rulesContext.activateCurrentRule();
        }
    }
}
