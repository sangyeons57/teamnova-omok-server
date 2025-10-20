package teamnova.omok.glue.game.session.states.manage;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionLifecycleAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionOutcomeAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionParticipantsAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionPostGameAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionPostGameRuntimeAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionTurnAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionTurnRuntimeAccess;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.modules.state_machine.interfaces.StateContext;

/**
 * Shared context passed to game session state implementations. Exposes the session and
 * runtime stores without owning mutable buffers itself.
 */
public final class GameSessionStateContext implements StateContext {
    private final GameSessionAccess access;
    private final GameSessionTurnRuntimeAccess turnRuntime;
    private final GameSessionPostGameRuntimeAccess postGameRuntime;

    public GameSessionStateContext(GameSession session) {
        Objects.requireNonNull(session, "session");
        this.access = session;
        this.turnRuntime = session;
        this.postGameRuntime = session;
    }

    public GameSessionAccess session() {
        return access;
    }

    public GameSessionBoardAccess board() {
        return access;
    }

    public GameSessionTurnAccess turns() {
        return access;
    }

    public GameSessionParticipantsAccess participants() {
        return access;
    }

    public GameSessionOutcomeAccess outcomes() {
        return access;
    }

    public GameSessionPostGameAccess postGame() {
        return access;
    }

    public GameSessionLifecycleAccess lifecycle() {
        return access;
    }

    public GameSessionRuleAccess rules() {
        return access;
    }

    public GameSessionTurnRuntimeAccess turnRuntime() {
        return turnRuntime;
    }

    public GameSessionPostGameRuntimeAccess postGameRuntime() {
        return postGameRuntime;
    }

    public GameSessionAccess view() {
        return access;
    }
}
