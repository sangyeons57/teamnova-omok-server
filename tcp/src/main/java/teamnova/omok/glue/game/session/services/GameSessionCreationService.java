package teamnova.omok.glue.game.session.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.modules.matching.models.MatchGroup;

/**
 * Stateless helpers for creating and registering new game sessions.
 */
public final class GameSessionCreationService {

    private GameSessionCreationService() {
    }

    public static void createFromGroup(GameSessionDependencies deps, MatchGroup group) {
        Objects.requireNonNull(deps, "deps");
        Objects.requireNonNull(group, "group");

        List<String> userIds = new ArrayList<>();
        group.tickets().forEach(ticket -> userIds.add(ticket.id()));
        GameSession session = new GameSession(userIds);

        Map<String, Integer> knownScores = new HashMap<>();
        group.tickets().forEach(ticket -> knownScores.put(ticket.id(), ticket.rating()));
        session.setRulesContext(deps.ruleManager().prepareRules(session, knownScores));

        deps.repository().save(session);
        deps.runtime().ensure(session);
        deps.messenger().broadcastJoin(session);
    }
}
