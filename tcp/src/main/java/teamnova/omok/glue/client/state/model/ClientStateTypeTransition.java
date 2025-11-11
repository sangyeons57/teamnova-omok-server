package teamnova.omok.glue.client.state.model;

import teamnova.omok.glue.client.state.manage.ClientStateType;

public record ClientStateTypeTransition(ClientStateType previousState, ClientStateType currentState) {
}
