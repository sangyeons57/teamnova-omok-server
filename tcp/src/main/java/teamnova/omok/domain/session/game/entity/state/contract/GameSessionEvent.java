package teamnova.omok.domain.session.game.entity.state.contract;

import teamnova.omok.game.PostGameDecisionType;

/**
 * Marker for a game session event.
 */
public interface GameSessionEvent {

    record Ready(String userId, long timestamp, long requestId) implements GameSessionEvent {}
    record Move (String userId, int x, int y, long timestamp, long requestId) implements GameSessionEvent {}
    record Timeout(int expectedTurnNumber, long timestamp) implements GameSessionEvent {}
    record PostGameDecision(String userId, PostGameDecisionType decision, long timestamp, long requestId) implements GameSessionEvent {}
    record DecisionTimeout(long triggerAt) implements GameSessionEvent{}
}
