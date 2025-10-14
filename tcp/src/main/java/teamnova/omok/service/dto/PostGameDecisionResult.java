package teamnova.omok.service.dto;

import teamnova.omok.game.PostGameDecisionType;
import teamnova.omok.domain.session.game.GameSession;

/**
 * Captures the evaluation of a player's post-game decision.
 */
public record PostGameDecisionResult(GameSession session,
                                     String userId,
                                     PostGameDecisionType decision,
                                     PostGameDecisionStatus status) {

    public static PostGameDecisionResult accepted(GameSession session,
                                                  String userId,
                                                  PostGameDecisionType decision) {
        return new PostGameDecisionResult(session, userId, decision, PostGameDecisionStatus.ACCEPTED);
    }

    public static PostGameDecisionResult rejected(GameSession session,
                                                  String userId,
                                                  PostGameDecisionStatus status) {
        return new PostGameDecisionResult(session, userId, null, status);
    }
}
