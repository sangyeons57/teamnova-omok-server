package teamnova.omok.glue.client.session.states.manage;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import teamnova.omok.modules.state_machine.models.StateName;

/**
 * Connection-level lifecycle phases for a managed client session.
 */
public enum ClientStateType {
    CONNECTED,
    AUTHENTICATED,
    MATCHING,
    IN_GAME,
    DISCONNECTED;

    private static final Map<StateName, ClientStateType> LOOKUP =
        Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ClientStateType::toStateName, type -> type));

    private final StateName stateName;

    ClientStateType() {
        this.stateName = StateName.of(name().toLowerCase());
    }

    public StateName toStateName() {
        return stateName;
    }

    public static ClientStateType fromStateName(StateName stateName) {
        ClientStateType type = LOOKUP.get(stateName);
        if (type == null) {
            throw new IllegalArgumentException("Unknown client state name: " + stateName.name());
        }
        return type;
    }
}
