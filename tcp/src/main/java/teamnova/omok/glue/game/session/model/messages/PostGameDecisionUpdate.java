package teamnova.omok.glue.game.session.model.messages;

import java.util.List;
import java.util.Map;

import teamnova.omok.glue.game.session.model.PostGameDecision;

/**
 * Broadcast payload capturing partial post-game decision progress.
 */
public record PostGameDecisionUpdate(Map<String, PostGameDecision> decisions,
                                     List<String> remaining) { }
