package teamnova.omok.glue.game.session.states.manage;

/**
 * Facade exposing focused helpers for manipulating {@link GameSessionStateContext}.
 */
public final class GameSessionStateContextService {

    private final GameSessionTurnContextService turn = new GameSessionTurnContextService();
    private final GameSessionPostGameContextService postGame = new GameSessionPostGameContextService();

    public GameSessionTurnContextService turn() {
        return turn;
    }

    public GameSessionPostGameContextService postGame() {
        return postGame;
    }
}
