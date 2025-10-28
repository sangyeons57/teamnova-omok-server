package teamnova.omok.glue.game.session.model.dto;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.DecisionTimeoutScheduler;
import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.GameScoreService;
import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.GameSessionRepository;
import teamnova.omok.glue.game.session.interfaces.GameSessionRuntime;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.manager.TurnTimeoutScheduler;
import teamnova.omok.glue.game.session.services.HiddenPlacementCoordinator;
import teamnova.omok.glue.game.session.services.TurnBudgetManager;
import teamnova.omok.glue.game.session.services.TurnOrderCoordinator;

public record GameSessionServices(GameBoardService boardService,
                                  GameTurnService turnService,
                                  GameScoreService scoreService,
                                  GameSessionMessenger messenger,
                                  HiddenPlacementCoordinator hiddenPlacementCoordinator,
                                  TurnOrderCoordinator turnOrderCoordinator,
                                  TurnBudgetManager turnBudgetManager,
                                  TurnTimeoutScheduler turnTimeoutScheduler,
                                  DecisionTimeoutScheduler decisionTimeoutScheduler,
                                  GameSessionRepository repository,
                                  GameSessionRuntime runtime) {
    public GameSessionServices {
        Objects.requireNonNull(boardService, "boardService");
        Objects.requireNonNull(turnService, "turnService");
        Objects.requireNonNull(scoreService, "scoreService");
        Objects.requireNonNull(messenger, "messenger");
        Objects.requireNonNull(hiddenPlacementCoordinator, "hiddenPlacementCoordinator");
        Objects.requireNonNull(turnOrderCoordinator, "turnOrderCoordinator");
        Objects.requireNonNull(turnBudgetManager, "turnBudgetManager");
        Objects.requireNonNull(turnTimeoutScheduler, "turnTimeoutScheduler");
        Objects.requireNonNull(decisionTimeoutScheduler, "decisionTimeoutScheduler");
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(runtime, "runtime");
    }
}
