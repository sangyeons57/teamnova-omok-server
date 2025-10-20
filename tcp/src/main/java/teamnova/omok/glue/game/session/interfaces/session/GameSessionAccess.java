package teamnova.omok.glue.game.session.interfaces.session;

public interface GameSessionAccess extends GameSessionBoardAccess,
        GameSessionParticipantsAccess,
        GameSessionLifecycleAccess,
        GameSessionRuleAccess,
        GameSessionTurnAccess,
        GameSessionOutcomeAccess,
        GameSessionPostGameAccess,
        GameSessionTurnRuntimeAccess,
        GameSessionPostGameRuntimeAccess {
}
