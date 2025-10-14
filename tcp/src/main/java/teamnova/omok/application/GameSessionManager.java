package teamnova.omok.application;

import java.util.*;

import teamnova.omok.application.register.GameSessionStateRegister;
import teamnova.omok.domain.matching.model.Group;
import teamnova.omok.domain.session.game.GameSession;
import teamnova.omok.domain.session.game.entity.state.GameSessionStateHolderService;
import teamnova.omok.service.RuleService;
import teamnova.omok.store.GameSessionStore;

/**
 * Central registry for active game sessions and their state managers.
 */
public class GameSessionManager {
    private final SessionMessagePublisher messagePublisher;
    private final GameSessionStateRegister stateRegister;
    private final GameSessionStateHolderService holderService;

    public GameSessionManager(SessionMessagePublisher messagePublisher, GameSessionStateRegister stateRegister, GameSessionStateHolderService holderService , RuleService ruleService) {
        this.messagePublisher = Objects.requireNonNull(messagePublisher, "messagePublisher");
        this.stateRegister = stateRegister;
        this.holderService = holderService;
    }

    public void createSession(GameSessionStore store, Group group) {
        List<String> userIds = new ArrayList<>();
        group.getTickets().forEach(t -> userIds.add(t.id));
        GameSession session = new GameSession(userIds);
        Map<String, Integer> knownScores = new HashMap<>();
        group.getTickets().forEach(ticket -> knownScores.put(ticket.id, ticket.rating));
        session.setRulesContext(ruleService.prepareRules(session, knownScores, RuleService.DEFAULT_RULE_SELECTION_COUNT));
        store.save(session);
        messagePublisher.broadcastJoin(session);
    }

    public void updateSessions(GameSessionStore store, long now) {
        for (GameSession session : store.findAll()) {
            session.update(holderService, this.stateRegister, now);
        }
    }
}
