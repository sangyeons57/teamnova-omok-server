package teamnova.omok.glue.game.session.model.messages;

import teamnova.omok.glue.game.session.model.GameSession;

/**
 * Broadcast payload for prompting post-game decisions.
 */
public record PostGameDecisionPrompt(GameSession session, long deadlineAt) { }
