package teamnova.omok.service.dto;

import java.util.List;
import java.util.Map;

import teamnova.omok.game.PostGameDecisionType;
import teamnova.omok.domain.session.game.GameSession;

/**
 * Broadcast payload capturing partial post-game decision progress.
 */
public record PostGameDecisionUpdate(GameSession session,
                                     Map<String, PostGameDecisionType> decisions,
                                     List<String> remaining) { }
