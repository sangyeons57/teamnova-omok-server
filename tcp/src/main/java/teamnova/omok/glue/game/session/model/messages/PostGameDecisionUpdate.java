package teamnova.omok.glue.game.session.model.messages;

import java.util.List;
import java.util.Map;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionParticipantsAccess;
import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.model.GameSession;

/**
 * Broadcast payload capturing partial post-game decision progress.
 */
public record PostGameDecisionUpdate(GameSessionParticipantsAccess session,
                                     Map<String, PostGameDecision> decisions,
                                     List<String> remaining) { }
