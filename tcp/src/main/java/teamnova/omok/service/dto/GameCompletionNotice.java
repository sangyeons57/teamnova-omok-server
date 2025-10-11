package teamnova.omok.service.dto;

import teamnova.omok.store.GameSession;

/**
 * Marker indicating a session has finished gameplay.
 */
public record GameCompletionNotice(GameSession session) { }
