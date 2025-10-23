package teamnova.omok.glue.game.session.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import teamnova.omok.glue.client.session.ClientSessionManager;
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

        GameSession session = new GameSession(group.ids());


        deps.repository().save(session);
        deps.runtime().ensure(session);

        // Bind participants' client sessions to this new game session for scoped messaging
        for (String uid : session.getUserIds()) {
            ClientSessionManager.getInstance()
                .findSession(uid)
                .ifPresent(handle -> handle.bindGameSession(session.sessionId()));
        }

        deps.messenger().broadcastJoin(session);

        Map<String, Integer> knownScores = new HashMap<>();
        group.tickets().forEach(ticket -> knownScores.put(ticket.id(), ticket.rating()));

        session.setRuleIds(deps.ruleManager().prepareRules(knownScores));
    }
}
