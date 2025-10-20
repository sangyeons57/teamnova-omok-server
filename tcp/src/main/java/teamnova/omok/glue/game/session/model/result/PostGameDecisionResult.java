package teamnova.omok.glue.game.session.model.result;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.model.GameSession;

/**
 * Captures the evaluation of a player's post-game decision.
 */
public record PostGameDecisionResult(GameSessionAccess session,
                                     String userId,
                                     PostGameDecision decision,
                                     PostGameDecisionStatus status) {

    public static PostGameDecisionResult accepted(GameSessionAccess session,
                                                  String userId,
                                                  PostGameDecision decision) {
        return new PostGameDecisionResult(session, userId, decision, PostGameDecisionStatus.ACCEPTED);
    }

    public static PostGameDecisionResult rejected(GameSessionAccess session,
                                                  String userId,
                                                  PostGameDecisionStatus status) {
        return new PostGameDecisionResult(session, userId, null, status);
    }
}
