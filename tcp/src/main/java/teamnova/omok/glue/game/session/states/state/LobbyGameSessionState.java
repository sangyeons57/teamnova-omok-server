package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.states.event.ReadyEvent;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Handles lobby behaviour prior to the game starting.
 */
public class LobbyGameSessionState implements BaseState {
    private final GameBoardService boardService;
    private final GameTurnService turnService;

    public LobbyGameSessionState(GameBoardService boardService, GameTurnService turnService) {
        this.boardService = Objects.requireNonNull(boardService, "boardService");
        this.turnService = Objects.requireNonNull(turnService, "turnService");
    }
    @Override
    public StateName name() {
        return GameSessionStateType.LOBBY.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEvent(I context, BaseEvent event) {
        if (!(context instanceof GameSessionStateContext gameContext)) {
            return StateStep.stay();
        }
        if (event instanceof ReadyEvent readyEvent) {
            return handleReady(gameContext, readyEvent);
        }
        return StateStep.stay();
    }

    private StateStep handleReady(GameSessionStateContext context,
                                  ReadyEvent event) {
        GameSession session = context.getSession();
        ReadyResult result;
        session.lock().lock();
        try {
            int playerIndex = session.playerIndexOf(event.userId());
            if (playerIndex < 0) {
                result = ReadyResult.invalid(session, event.userId());
            } else {
                boolean changed = session.markReady(event.userId());
                boolean allReady = session.allReady();
                boolean startedNow = false;
                GameTurnService.TurnSnapshot snapshot = null;
                if (allReady && !session.isGameStarted()) {
                    startedNow = true;
                    session.markGameStarted(event.timestamp());
                    session.resetOutcomes();
                    boardService.reset(context.getSession());
                    snapshot = turnService
                        .start(context.getSession(), session.getUserIds(), event.timestamp());
                } else if (session.isGameStarted()) {
                    snapshot = turnService.snapshot(context.getSession());
                }
                result = new ReadyResult(
                    session,
                    true,
                    changed,
                    allReady,
                    startedNow,
                    snapshot,
                    event.userId()
                );
            }
        } finally {
            session.lock().unlock();
        }

        context.pendingReadyResult(result);
        if (!result.validUser()) {
            return StateStep.stay();
        }
        if (result.gameStartedNow()) {
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }
        return StateStep.stay();
    }
}
