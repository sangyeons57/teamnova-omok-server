package teamnova.omok.glue.game.session.model.messages;

import teamnova.omok.glue.game.session.model.GameSession;

/**
 * Marker indicating a session has finished gameplay.
 */
public record GameCompletionNotice(GameSession session) { }
