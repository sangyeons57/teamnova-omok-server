package teamnova.omok.glue.rule.runtime;

import java.util.Objects;

import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.rule.api.RuleTriggerKind;

/**
 * Runtime data supplied when rules are invoked for a specific lifecycle event.
 */
public record RuleRuntimeContext(GameSessionServices services,
                                 GameSessionStateContextService contextService,
                                 GameSessionStateContext stateContext,
                                 TurnSnapshot turnSnapshot,
                                 RuleTriggerKind triggerKind) {
    public RuleRuntimeContext {
        Objects.requireNonNull(services, "services");
        Objects.requireNonNull(contextService, "contextService");
        Objects.requireNonNull(stateContext, "stateContext");
        Objects.requireNonNull(triggerKind, "triggerKind");
    }
}
