package teamnova.omok.application.register;

import teamnova.omok.domain.session.game.entity.state.contract.GameSessionState;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateType;
import teamnova.omok.domain.session.game.entity.state.state.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class GameSessionStateRegister {
    private final Map<GameSessionStateType, GameSessionState> registrations = new EnumMap<>(GameSessionStateType.class);

    public GameSessionStateRegister() {

    }

    public void configure() {
        register(new LobbyGameSessionState());
        register(new TurnWaitingState());
        register(new MoveValidatingState());
        register(new MoveApplyingState());
        register(new OutcomeEvaluatingState());
        register(new TurnFinalizingState());
        register(new PostGameDecisionWaitingState());
        register(new PostGameDecisionResolvingState());
        register(new SessionRematchPreparingState());
        register(new SessionTerminatingState());
        register(new CompletedGameSessionState());
    }

    public void register(GameSessionState state) {
        registrations.put(state.type(), state);
    }

    public GameSessionState get(GameSessionStateType type) {
        return registrations.get(type);
    }

    public Set<GameSessionStateType> types() {
        return registrations.keySet();
    }
}
