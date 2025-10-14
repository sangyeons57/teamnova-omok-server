package teamnova.omok.usecase;

public sealed interface UResult<T extends UseCaseResponse> {
    record Success<T extends UseCaseResponse>(T value)  implements UResult<T> {}
    record Failure<T extends UseCaseResponse>(String message) implements UResult<T> { }
}
