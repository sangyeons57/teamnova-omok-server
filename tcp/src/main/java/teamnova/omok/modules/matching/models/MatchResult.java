package teamnova.omok.modules.matching.models;

public sealed interface MatchResult permits MatchResult.Success, MatchResult.Fail {
    record Success(MatchGroup group) implements MatchResult {}
    record Fail(String message) implements MatchResult {}

    static Fail fail(String message) { return new Fail(message); }
    static Success success(MatchGroup group) { return new Success(group); }
}
