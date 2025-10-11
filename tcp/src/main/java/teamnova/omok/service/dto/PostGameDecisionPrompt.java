package teamnova.omok.service.dto;

import teamnova.omok.store.GameSession;

/**
 * Broadcast payload for prompting post-game decisions.
 */
public record PostGameDecisionPrompt(GameSession session, long deadlineAt) { }
