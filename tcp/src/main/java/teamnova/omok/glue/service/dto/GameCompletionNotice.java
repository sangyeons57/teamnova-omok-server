package teamnova.omok.glue.service.dto;

import teamnova.omok.glue.store.GameSession;

/**
 * Marker indicating a session has finished gameplay.
 */
public record GameCompletionNotice(GameSession session) { }
