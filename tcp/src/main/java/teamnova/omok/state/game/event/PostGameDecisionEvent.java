package teamnova.omok.state.game.event;

import teamnova.omok.game.PostGameDecision;

public record PostGameDecisionEvent(String userId,
                                    PostGameDecision decision,
                                    long timestamp,
                                    long requestId) implements GameSessionEvent {

    @Override
    public GameSessionEventType type() {
        return GameSessionEventType.POST_GAME_DECISION;
    }
}
