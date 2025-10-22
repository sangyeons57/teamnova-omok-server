package teamnova.omok.glue.game.session.model.dto;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.GameScoreService;
import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.manager.TurnTimeoutScheduler;
import teamnova.omok.glue.game.session.interfaces.DecisionTimeoutScheduler;

/**
 * 게임 세션 처리에 필요한 서비스 모음.
 */
public record GameSessionServices(GameBoardService boardService,
                                  GameTurnService turnService,
                                  GameScoreService scoreService,
                                  GameSessionMessenger messenger,
                                  TurnTimeoutScheduler turnTimeoutScheduler,
                                  DecisionTimeoutScheduler decisionTimeoutScheduler) {
    public GameSessionServices {
        Objects.requireNonNull(boardService, "boardService");
        Objects.requireNonNull(turnService, "turnService");
        Objects.requireNonNull(scoreService, "scoreService");
        Objects.requireNonNull(messenger, "messenger");
        Objects.requireNonNull(turnTimeoutScheduler, "turnTimeoutScheduler");
        Objects.requireNonNull(decisionTimeoutScheduler, "decisionTimeoutScheduler");
    }
}
