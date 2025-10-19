package teamnova.omok.glue.game.session.services;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.DecisionTimeoutScheduler;
import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.GameSessionRepository;
import teamnova.omok.glue.game.session.interfaces.GameSessionRuntime;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.manager.TurnTimeoutScheduler;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.rule.RuleManager;

/**
 * Game session infrastructure references that can be shared across stateless services.
 */
public record GameSessionDependencies(
    GameSessionRepository repository,
    GameSessionRuntime runtime,
    GameTurnService turnService,
    GameSessionMessenger messenger,
    TurnTimeoutScheduler turnTimeoutScheduler,
    DecisionTimeoutScheduler decisionTimeoutScheduler,
    RuleManager ruleManager,
    GameSessionStateContextService contextService
) {
    public GameSessionDependencies {
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(runtime, "runtime");
        Objects.requireNonNull(turnService, "turnService");
        Objects.requireNonNull(messenger, "messenger");
        Objects.requireNonNull(turnTimeoutScheduler, "turnTimeoutScheduler");
        Objects.requireNonNull(decisionTimeoutScheduler, "decisionTimeoutScheduler");
        Objects.requireNonNull(ruleManager, "ruleManager");
        Objects.requireNonNull(contextService, "contextService");
    }
}
