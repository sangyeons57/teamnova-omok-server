package teamnova.omok.glue.game.session.interfaces.session;

public interface GameSessionLifecycleAccess extends GameSessionAccessInterface {
    long getCreatedAt();
    boolean isGameStarted();
    long getGameStartedAt();
    void markGameStarted(long startedAt);
    long getGameEndedAt();
    void markGameFinished(long endedAt, int turnCount);
    int getCompletedTurnCount();
    long getGameDurationMillis();
}
