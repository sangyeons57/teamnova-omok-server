package teamnova.omok.glue.game.session.model.result;

import teamnova.omok.glue.game.session.model.PostGameDecision;

/**
 * Captures the evaluation of a player's post-game decision.
 */
public record PostGameDecisionResult(String userId,
                                     long requestId,
                                     PostGameDecision decision,
                                     PostGameDecisionStatus status) {

    public static PostGameDecisionResult accepted(String userId,
                                                  long requestId,
                                                  PostGameDecision decision) {
        return new PostGameDecisionResult(userId, requestId, decision, PostGameDecisionStatus.ACCEPTED);
    }

    public static PostGameDecisionResult rejected(String userId,
                                                  PostGameDecisionStatus status,
                                                  long requestId) {
        return new PostGameDecisionResult(userId, requestId, null, status);
    }
}
