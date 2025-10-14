package teamnova.omok;

import teamnova.omok.application.SessionMessagePublisher;
import teamnova.omok.handler.register.DefaultHandlerRegistry;
import teamnova.omok.infra.InfraContainer;
import teamnova.omok.application.register.GameSessionStateRegister;
import teamnova.omok.application.GameSessionManager;

public class CompositionRoot {

    private final GameSessionManager gameSessionManager;

    private final DefaultHandlerRegistry handlerRegistry;
    private final GameSessionStateRegister gameSessionStateRegistry;

    private final InfraContainer infraContainer;

    private final SessionMessagePublisher publisher;

    public CompositionRoot() {
        this.infraContainer = new InfraContainer();

        this.handlerRegistry = new DefaultHandlerRegistry(infraContainer);

        this.gameSessionManager = new GameSessionManager();

        this.gameSessionStateRegistry = new GameSessionStateRegister();

        this.publisher = new SessionMessagePublisher();
    }

    public DefaultHandlerRegistry handlerRegistry() {
        return this.handlerRegistry;
    }
    public GameSessionManager gameSessionManager() {
        return this.gameSessionManager;
    }
    public GameSessionStateRegister gameSessionStateRegistry() {
        return this.gameSessionStateRegistry;
    }
    public InfraContainer infraContainer() {
        return this.infraContainer;
    }
    public SessionMessagePublisher publisher() { return this.publisher; }
}
