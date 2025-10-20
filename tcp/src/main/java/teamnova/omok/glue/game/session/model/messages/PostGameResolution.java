package teamnova.omok.glue.game.session.model.messages;

import java.util.List;

/**
 * Describes how a post-game stage concluded.
 */
public record PostGameResolution(ResolutionType type,
                                 List<String> rematchParticipants,
                                 List<String> disconnected) {

    public enum ResolutionType {
        REMATCH,
        TERMINATE
    }

    public static PostGameResolution rematch(List<String> participants,
                                             List<String> disconnected) {
        return new PostGameResolution(ResolutionType.REMATCH,
            List.copyOf(participants),
            List.copyOf(disconnected));
    }

    public static PostGameResolution terminate(List<String> disconnected) {
        return new PostGameResolution(ResolutionType.TERMINATE,
            List.of(),
            List.copyOf(disconnected));
    }
}
