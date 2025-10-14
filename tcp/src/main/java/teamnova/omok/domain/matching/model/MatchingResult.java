package teamnova.omok.domain.matching.model;


public interface MatchingResult {
    // 매칭 성공
    public record Success(Group group) implements MatchingResult {}
    // 매칭 실패
    public record Fail(String message) implements MatchingResult {}

    public static MatchingResult.Fail fail(String message) {
        return new MatchingResult.Fail(message);
    }

    public static MatchingResult.Success success(Group group) {
        return new MatchingResult.Success(group);
    }
}
