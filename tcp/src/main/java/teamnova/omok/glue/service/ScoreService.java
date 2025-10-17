package teamnova.omok.glue.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import teamnova.omok.core.nio.ClientSession;
import teamnova.omok.core.nio.ClientSessions;
import teamnova.omok.glue.data.MysqlService;
import teamnova.omok.glue.game.PlayerResult;
import teamnova.omok.glue.manager.DataManager;
import teamnova.omok.glue.store.GameSession;
import teamnova.omok.modules.formula.FormulaGateway;
import teamnova.omok.modules.formula.models.FormulaRequest;
import teamnova.omok.modules.formula.models.FormulaVariables;
import teamnova.omok.modules.formula.models.MatchOutcome;
import teamnova.omok.modules.formula.models.PreparedFormula;
import teamnova.omok.modules.formula.terminals.FinalizeTerminal;

/**
 * Applies post-game score adjustments to players using {@link MysqlService}.
 * Delegates the score delta computation to the reusable score module.
 */
public final class ScoreService {
    private static final double STAGE_REQUIREMENT_POINTS = 100.0; // 단계 당  요구 점수
    private static final double STAGE_REQUIREMENT_WINS = 10.0;    // 단계 당 요구 승리 횟수
    private static final double INITIAL_BONUS_STAGE_LIMIT = 5.0;  // 초기 보너스 제공 단계 수 (5단계 = 500점)
    private static final double LOSS_RATIO_DIVISOR = 500.0;       // 패배시 소수점 내림(플레이어 점수 / 500)
    private static final double DISCONNECTED_PENALTY = 0.0;       // 명시된 값 없음 -> 기본 0 적용

    private final FormulaGateway.Handle formulaHandle;

    public ScoreService() {
        PreparedFormula prepared = FormulaGateway.pipeline()
                .bootstrap(STAGE_REQUIREMENT_POINTS, STAGE_REQUIREMENT_WINS, INITIAL_BONUS_STAGE_LIMIT, DISCONNECTED_PENALTY)
                .resolveOutcome(STAGE_REQUIREMENT_POINTS, STAGE_REQUIREMENT_WINS, INITIAL_BONUS_STAGE_LIMIT, LOSS_RATIO_DIVISOR)
                .clampWinResult(STAGE_REQUIREMENT_POINTS, STAGE_REQUIREMENT_WINS)
                .applyDisconnectedPenalty(DISCONNECTED_PENALTY)
                .build(new FinalizeTerminal());
        this.formulaHandle = FormulaGateway.wrapPrepared(prepared);
    }

    public int calculateScoreDelta(GameSession session, String userId) {
        List<String> participants = session.getUserIds();
        PlayerResult result = session.outcomeFor(userId);
        MatchOutcome outcome = mapOutcome(result);
        boolean isDisconnected = session.disconnectedUsersView().contains(userId);
        Map<String, Integer> currentScores = loadCurrentScores(participants);

        int playerScore = currentScores.getOrDefault(userId, 0);
        int opponentScore = selectOpponentScore(userId, participants, currentScores);

        ClientSession clientSession = ClientSessions.findAuthenticated(userId);
        int streakValue = 0;
        int totalWins = 0;
        int totalLosses = 0;
        int totalDraws = 0;
        if (clientSession != null) {
            streakValue = clientSession.registerOutcome(result);
            totalWins = clientSession.totalWins();
            totalLosses = clientSession.totalLosses();
            totalDraws = clientSession.totalDraws();
        }

        FormulaRequest request = FormulaRequest.builder()
                .put(FormulaVariables.OUTCOME, outcome)
                .put(FormulaVariables.DISCONNECTED, isDisconnected)
                .put(FormulaVariables.PLAYER_SCORE, playerScore)
                .put(FormulaVariables.OPPONENT_SCORE, opponentScore)
                .put(FormulaVariables.WIN_STREAK, streakValue)
                .put(FormulaVariables.TOTAL_WINS, totalWins)
                .put(FormulaVariables.TOTAL_LOSSES, totalLosses)
                .put(FormulaVariables.TOTAL_DRAWS, totalDraws)
                .build();

        return formulaHandle.evaluate(request).delta();
    }

    private Map<String, Integer> loadCurrentScores(List<String> userIds) {
        Map<String, Integer> scores = new HashMap<>();
        for (String userId : userIds) {
            int score = DataManager.getInstance().getUserScore(userId, 0).score();
            scores.put(userId, score);
        }
        return scores;
    }

    private int selectOpponentScore(String userId,
                                    List<String> participants,
                                    Map<String, Integer> scores) {
        for (String other : participants) {
            if (!Objects.equals(other, userId)) {
                return scores.getOrDefault(other, 0);
            }
        }
        return scores.getOrDefault(userId, 0);
    }

    private MatchOutcome mapOutcome(PlayerResult result) {
        if (result == null) {
            return MatchOutcome.PENDING;
        }
        return switch (result) {
            case WIN -> MatchOutcome.WIN;
            case LOSS -> MatchOutcome.LOSS;
            case DRAW -> MatchOutcome.DRAW;
            case PENDING -> MatchOutcome.PENDING;
        };
    }
}
