package teamnova.omok.usecase;

public interface UseCase<I extends UseCaseRequest, O extends UseCaseResponse> {

    UResult<O> execute(I request);

}
