package teamnova.omok.domain.session.game.entity.turn;

public interface TurnReadable {
    public boolean isExpired(long now);

    public Integer currentPlayerIndex();
}
