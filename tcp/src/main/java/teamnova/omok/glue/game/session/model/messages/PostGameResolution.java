package teamnova.omok.glue.game.session.model.messages;

import java.util.List;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.GameSession;

/**
 * Describes how a post-game stage concluded.
 */
public record PostGameResolution(GameSessionAccess session,
                                 ResolutionType type,
                                 List<String> rematchParticipants,
                                 List<String> disconnected) {

    public enum ResolutionType {
        REMATCH,
        TERMINATE
    }

    public static PostGameResolution rematch(GameSessionAccess session,
                                             List<String> participants,
                                             List<String> disconnected) {
        return new PostGameResolution(session,
            ResolutionType.REMATCH,
            List.copyOf(participants),
            List.copyOf(disconnected));
    }

    public static PostGameResolution terminate(GameSessionAccess session,
                                               List<String> disconnected) {
        return new PostGameResolution(session,
            ResolutionType.TERMINATE,
            List.of(),
            List.copyOf(disconnected));
    }
}
