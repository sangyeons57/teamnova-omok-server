package teamnova.omok.service.dto;

import teamnova.omok.domain.session.game.GameSession;

/**
 * Broadcast payload for prompting post-game decisions.
 */
public record PostGameDecisionPrompt(GameSession session, long deadlineAt) { }
