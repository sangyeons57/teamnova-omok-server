package teamnova.omok.glue.service.dto;

import teamnova.omok.glue.store.GameSession;

/**
 * Broadcast payload for prompting post-game decisions.
 */
public record PostGameDecisionPrompt(GameSession session, long deadlineAt) { }
