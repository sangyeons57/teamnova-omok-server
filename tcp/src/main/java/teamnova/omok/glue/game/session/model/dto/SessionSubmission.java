package teamnova.omok.glue.game.session.model.dto;

import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.states.GameStateHub;

/**
 * 상태 머신 이벤트 제출에 필요한 세션 컨텍스트.
 */
public record SessionSubmission(GameSession session,
                                GameStateHub manager,
                                long timestamp) { }
