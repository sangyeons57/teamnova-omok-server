package teamnova.omok.usecase;

public abstract class DefaultUseCase<I extends UseCaseRequest, O extends UseCaseResponse> implements UseCase<I, O>{

    @Override
    public UResult<O> execute(I request) {
        return null;
    }
}
