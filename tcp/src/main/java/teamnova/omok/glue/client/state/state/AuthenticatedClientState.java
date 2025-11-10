package teamnova.omok.glue.client.state.state;

import teamnova.omok.glue.client.state.event.DisconnectClientEvent;
import teamnova.omok.glue.client.state.event.EnterGameClientEvent;
import teamnova.omok.glue.client.state.event.ResetClientEvent;
import teamnova.omok.glue.client.state.event.StartMatchingClientEvent;
import teamnova.omok.glue.client.state.manage.ClientStateContext;
import teamnova.omok.glue.client.state.manage.ClientStateType;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * State once the client has successfully authenticated but is not yet in a game.
 */
public final class AuthenticatedClientState implements BaseState {
    @Override
    public StateName name() {
        return ClientStateType.AUTHENTICATED.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEvent(I context, BaseEvent event) {
        ClientStateContext clientContext = (ClientStateContext) context;
        if (event instanceof StartMatchingClientEvent startMatching) {
            clientContext.prepareMatching(startMatching.matchSizes(), startMatching.rating(), startMatching.requestId());
            return StateStep.transition(ClientStateType.MATCHING.toStateName());
        }
        if (event instanceof EnterGameClientEvent enterGame) {
            clientContext.attachGame(enterGame.gameStateManager());
            return StateStep.transition(ClientStateType.IN_GAME.toStateName());
        }
        if (event instanceof DisconnectClientEvent) {
            clientContext.clearGame();
            return StateStep.transition(ClientStateType.DISCONNECTED.toStateName());
        }
        if (event instanceof ResetClientEvent) {
            clientContext.clearGame();
            return StateStep.transition(ClientStateType.CONNECTED.toStateName());
        }
        return StateStep.stay();
    }
}
