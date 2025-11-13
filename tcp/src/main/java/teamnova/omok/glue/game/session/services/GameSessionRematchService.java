package teamnova.omok.glue.game.session.services;

import java.util.List;
import java.util.Objects;

import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.GameSessionRepository;
import teamnova.omok.glue.game.session.interfaces.GameSessionRuntime;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.glue.rule.runtime.RuleManager;

/**
 * Helper for creating and broadcasting rematch sessions.
 */
public final class GameSessionRematchService {
    private GameSessionRematchService() { }

    public static GameSession createAndBroadcast(GameSessionServices services,
                                                 GameSessionAccess previous,
                                                 List<String> participants) {
        Objects.requireNonNull(services, "services");
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(participants, "participants");

        GameSessionRepository repository = services.repository();
        GameSessionRuntime runtime = services.runtime();
        GameSessionMessenger messenger = services.messenger();

        GameSession rematch = new GameSession(participants);
        rematch.setRuleIds(RuleManager.getInstance().prepareRules());
        repository.save(rematch);
        runtime.ensure(rematch);
        // Notify both sessions: a rematch has been created
        messenger.broadcastRematchStarted(previous, rematch, participants);
        return rematch;
    }

    public static void finalizeAndJoin(GameSessionServices services, GameSession rematch) {
        Objects.requireNonNull(services, "services");
        if (rematch == null) {
            return;
        }
        GameSessionMessenger messenger = services.messenger();
        GameStateHub manager = services.runtime().ensure(rematch);
        // Bind participants' client sessions to the new rematch for session-scoped routing
        for (String uid : rematch.getUserIds()) {
            ClientSessionManager.getInstance()
                .findSession(uid)
                .ifPresent(handle -> {
                    handle.bindGameSession(rematch.sessionId());
                    handle.enterGameSession(manager);
                });
        }

        // Broadcast join for the new session so clients can transition immediately
        messenger.broadcastJoin(rematch);
    }
}
