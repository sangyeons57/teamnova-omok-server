package teamnova.omok.service.dto;

import teamnova.omok.domain.session.game.GameSession;

/**
 * Marker indicating a session has finished gameplay.
 */
public record GameCompletionNotice(GameSession session) { }
