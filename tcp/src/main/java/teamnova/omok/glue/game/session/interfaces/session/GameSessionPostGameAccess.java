package teamnova.omok.glue.game.session.interfaces.session;

import teamnova.omok.glue.game.session.model.PostGameDecision;

import java.util.Map;
import java.util.Set;

public interface GameSessionPostGameAccess extends GameSessionAccessInterface{
    boolean recordPostGameDecision(String userId, PostGameDecision decision);
    boolean hasPostGameDecision(String userId);
    Map<String, PostGameDecision> postGameDecisionsView();
    Set<String> rematchRequestsView();
    void resetPostGameDecisions();
}
