package teamnova.omok.glue.service.dto;

import teamnova.omok.glue.game.session.model.GameSession;

/**
 * Marker indicating a session has finished gameplay.
 */
public record GameCompletionNotice(GameSession session) { }
