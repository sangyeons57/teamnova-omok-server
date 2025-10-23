package teamnova.omok.glue.game.session.services;

import java.util.List;
import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.GameSessionRepository;
import teamnova.omok.glue.game.session.interfaces.GameSessionRuntime;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;

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
        repository.save(rematch);
        runtime.ensure(rematch);
        // Notify both sessions: a rematch has been created
        messenger.broadcastRematchStarted(previous, rematch, participants);
        // Optionally, also broadcast join for the new session so clients can transition immediately
        messenger.broadcastJoin(rematch);
        return rematch;
    }
}
