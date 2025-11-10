package teamnova.omok.glue.client.state.event;

import java.util.Set;

import teamnova.omok.modules.state_machine.interfaces.BaseEvent;

public record StartMatchingClientEvent(Set<Integer> matchSizes, int rating, long requestId) implements BaseEvent {
    public StartMatchingClientEvent {
        matchSizes = matchSizes == null ? Set.of() : Set.copyOf(matchSizes);
    }
}
