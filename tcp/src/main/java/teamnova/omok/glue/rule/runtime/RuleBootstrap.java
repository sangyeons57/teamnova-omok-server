package teamnova.omok.glue.rule.runtime;

import java.util.Objects;

import teamnova.omok.glue.rule.rules.AimMissRule;
import teamnova.omok.glue.rule.rules.BlackViewRule;
import teamnova.omok.glue.rule.rules.BlockerBanRule;
import teamnova.omok.glue.rule.rules.BlockerSummonRule;
import teamnova.omok.glue.rule.rules.ColosseumRule;
import teamnova.omok.glue.rule.rules.DelayedRevealRule;
import teamnova.omok.glue.rule.rules.EvolutionRule;
import teamnova.omok.glue.rule.rules.GoCaptureRule;
import teamnova.omok.glue.rule.rules.InfectionRule;
import teamnova.omok.glue.rule.rules.JokerPromotionRule;
import teamnova.omok.glue.rule.rules.JokerSummonRule;
import teamnova.omok.glue.rule.rules.LuckySevenRule;
import teamnova.omok.glue.rule.rules.LowDensityPurgeRule;
import teamnova.omok.glue.rule.rules.MirrorBoardRule;
import teamnova.omok.glue.rule.rules.NewPlayerRule;
import teamnova.omok.glue.rule.rules.ProtectiveZoneRule;
import teamnova.omok.glue.rule.rules.RandomMoveRule;
import teamnova.omok.glue.rule.rules.RandomPlacementRule;
import teamnova.omok.glue.rule.rules.ReversiConversionRule;
import teamnova.omok.glue.rule.rules.RoundTripTurnsRule;
import teamnova.omok.glue.rule.rules.SequentialConversionRule;
import teamnova.omok.glue.rule.rules.SixInRowRule;
import teamnova.omok.glue.rule.rules.SpeedGame2Rule;
import teamnova.omok.glue.rule.rules.SpeedGameRule;
import teamnova.omok.glue.rule.rules.StoneConversionRule;
import teamnova.omok.glue.rule.rules.TenChainEliminationRule;
import teamnova.omok.glue.rule.rules.TurnOrderShuffleRule;

public class RuleBootstrap {

    public void registerDefaults(RuleRegistry ruleRegistry) {
        Objects.requireNonNull(ruleRegistry, "ruleRegistry");
        ruleRegistry.register(new StoneConversionRule());
        ruleRegistry.register(new SpeedGameRule());
        ruleRegistry.register(new BlockerSummonRule());
        ruleRegistry.register(new JokerSummonRule());
        ruleRegistry.register(new GoCaptureRule());
        ruleRegistry.register(new SequentialConversionRule());
        ruleRegistry.register(new ReversiConversionRule());
        ruleRegistry.register(new SixInRowRule());
        ruleRegistry.register(new BlackViewRule());
        ruleRegistry.register(new RandomMoveRule());
        ruleRegistry.register(new InfectionRule());
        ruleRegistry.register(new NewPlayerRule());
        ruleRegistry.register(new ProtectiveZoneRule());
        ruleRegistry.register(new MirrorBoardRule());
        ruleRegistry.register(new BlockerBanRule());
        ruleRegistry.register(new JokerPromotionRule());
        ruleRegistry.register(new LuckySevenRule());
        ruleRegistry.register(new TenChainEliminationRule());
        ruleRegistry.register(new AimMissRule());
        ruleRegistry.register(new ColosseumRule());
        ruleRegistry.register(new EvolutionRule());
        ruleRegistry.register(new SpeedGame2Rule());
        ruleRegistry.register(new DelayedRevealRule());
        ruleRegistry.register(new LowDensityPurgeRule());
        ruleRegistry.register(new RandomPlacementRule());
        ruleRegistry.register(new TurnOrderShuffleRule());
        ruleRegistry.register(new RoundTripTurnsRule());
    }
}
