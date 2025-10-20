package teamnova.omok.glue.game.session.states.signal;

import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.rule.RulesContext;
import teamnova.omok.modules.state_machine.interfaces.StateSignalListener;
import teamnova.omok.modules.state_machine.models.LifecycleEventKind;
import teamnova.omok.modules.state_machine.models.StateName;

import java.util.Set;

public class TurnFinalizingSignal implements StateSignalListener {
    private final GameSessionStateContext context;
    private final GameSessionStateContextService contextService;
    private final GameSessionServices services;

    public TurnFinalizingSignal(GameSessionStateContext context, GameSessionStateContextService contextService, GameSessionServices services) {
        this.context = context;
        this.contextService = contextService;
        this.services = services;
    }

    private final Set<StateName> states = Set.of(GameSessionStateType.TURN_FINALIZING.toStateName());
    private final Set<LifecycleEventKind> events = Set.of(LifecycleEventKind.ON_START);

    @Override public Set<StateName> states() { return states; }
    @Override public Set<LifecycleEventKind> events() { return events; }
    @Override
    public void onSignal(StateName state, LifecycleEventKind kind) {

        GameSessionStateType targetType = GameSessionStateType.stateNameLookup(state);

        if (targetType == null) return;
        RulesContext rulesContext = context.session().getRulesContext();
        if (rulesContext == null) {
            return;
        }
        rulesContext.attachServices(services);
        rulesContext.attachContextService(contextService);
        rulesContext.attachStateContext(context);
        if (rulesContext.setCurrentRuleByGameState(targetType)) {
            rulesContext.activateCurrentRule();
        }
    }
}
