package teamnova.omok.glue.client.state.event;

import teamnova.omok.modules.state_machine.interfaces.BaseEvent;

/**
 * Signals that the client state should revert to the default connected state.
 */
public record ResetClientEvent() implements BaseEvent {
}
