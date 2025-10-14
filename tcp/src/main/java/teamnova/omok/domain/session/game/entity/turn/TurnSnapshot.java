package teamnova.omok.domain.session.game.entity.turn;

public record TurnSnapshot(int currentPlayerIndex, String currentPlayerId, int turnNumber,
                           long turnStartAt, long turnEndAt) { }
