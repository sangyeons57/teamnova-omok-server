package teamnova.omok.glue.service.dto;

import java.util.List;

import teamnova.omok.glue.store.GameSession;

/**
 * Describes how a post-game stage concluded.
 */
public record PostGameResolution(GameSession session,
                                 ResolutionType type,
                                 List<String> rematchParticipants,
                                 List<String> disconnected) {

    public enum ResolutionType {
        REMATCH,
        TERMINATE
    }

    public static PostGameResolution rematch(GameSession session,
                                             List<String> participants,
                                             List<String> disconnected) {
        return new PostGameResolution(session,
            ResolutionType.REMATCH,
            List.copyOf(participants),
            List.copyOf(disconnected));
    }

    public static PostGameResolution terminate(GameSession session,
                                               List<String> disconnected) {
        return new PostGameResolution(session,
            ResolutionType.TERMINATE,
            List.of(),
            List.copyOf(disconnected));
    }
}
