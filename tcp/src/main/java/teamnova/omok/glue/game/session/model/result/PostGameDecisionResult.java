package teamnova.omok.glue.game.session.model.result;

import teamnova.omok.glue.game.session.model.PostGameDecision;

/**
 * Captures the evaluation of a player's post-game decision.
 */
public record PostGameDecisionResult(String userId,
                                     PostGameDecision decision,
                                     PostGameDecisionStatus status) {

    public static PostGameDecisionResult accepted(String userId,
                                                  PostGameDecision decision) {
        return new PostGameDecisionResult(userId, decision, PostGameDecisionStatus.ACCEPTED);
    }

    public static PostGameDecisionResult rejected(String userId,
                                                  PostGameDecisionStatus status) {
        return new PostGameDecisionResult(userId, null, status);
    }
}
