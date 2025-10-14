package teamnova.omok.glue.state.game.state;

import teamnova.omok.glue.service.TurnService;
import teamnova.omok.glue.service.dto.ReadyResult;
import teamnova.omok.glue.state.game.event.ReadyEvent;
import teamnova.omok.glue.state.game.manage.GameSessionStateContext;
import teamnova.omok.glue.state.game.manage.GameSessionStateType;
import teamnova.omok.glue.store.GameSession;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Handles lobby behaviour prior to the game starting.
 */
public class LobbyGameSessionState implements BaseState {
    @Override
    public StateName name() {
        return GameSessionStateType.LOBBY.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        return StateStep.stay();
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
        GameSession session = context.session();
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
                TurnService.TurnSnapshot snapshot = null;
                if (allReady && !session.isGameStarted()) {
                    startedNow = true;
                    session.markGameStarted(event.timestamp());
                    session.resetOutcomes();
                    context.boardService().reset(session.getBoardStore());
                    snapshot = context.turnService()
                        .start(session.getTurnStore(), session.getUserIds(), event.timestamp());
                } else if (session.isGameStarted()) {
                    snapshot = context.turnService()
                        .snapshot(session.getTurnStore(), session.getUserIds());
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
