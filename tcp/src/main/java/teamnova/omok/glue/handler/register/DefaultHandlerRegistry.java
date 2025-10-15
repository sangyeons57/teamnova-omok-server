package teamnova.omok.glue.handler.register;

import teamnova.omok.glue.handler.AuthHandler;
import teamnova.omok.glue.handler.PingPongHandler;
import teamnova.omok.glue.message.decoder.HelloWorldDecoder;
import teamnova.omok.glue.handler.dispatcher.Dispatcher;
import teamnova.omok.glue.handler.HelloWorldHandler;
import teamnova.omok.glue.message.decoder.StringDecoder;
import teamnova.omok.glue.handler.JoinMatchHandler;
import teamnova.omok.glue.handler.LeaveInGameSessionHandler;
import teamnova.omok.glue.handler.LeaveMatchHandler;
import teamnova.omok.glue.handler.PostGameDecisionHandler;
import teamnova.omok.glue.handler.PlaceStoneHandler;
import teamnova.omok.glue.handler.ReadyInGameSessionHandler;
import teamnova.omok.glue.service.ServiceContainer;

import java.util.function.Supplier;

/**
 * Registers built-in handlers used by the server for testing/demo purposes.
 */
public final class DefaultHandlerRegistry implements HandlerRegistry {
    private final HelloWorldDecoder helloWorldDecoder;
    private final StringDecoder stringDecoder;

    private Dispatcher dispatcher;;

    public DefaultHandlerRegistry() {
        this.helloWorldDecoder = new HelloWorldDecoder();
        this.stringDecoder = new StringDecoder();
    }

    @Override
    public void configure(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
        register(Type.HELLO, new HelloWorldHandler(helloWorldDecoder));
        register(Type.AUTH, new AuthHandler(stringDecoder, ServiceContainer.getInstance().getDotenvService()));
        register(Type.PINGPONG, new PingPongHandler());
        register(Type.JOIN_MATCH, new JoinMatchHandler(stringDecoder));
        register(Type.LEAVE_MATCH, new LeaveMatchHandler());
        register(Type.LEAVE_IN_GAME_SESSION, new LeaveInGameSessionHandler());
        register(Type.READY_IN_GAME_SESSION, new ReadyInGameSessionHandler());
        register(Type.PLACE_STONE, new PlaceStoneHandler(stringDecoder));
        register(Type.POST_GAME_DECISION, new PostGameDecisionHandler(stringDecoder));
    }

    public void register(Type type, FrameHandler frameHandler ) {
        if (dispatcher == null) {
            System.err.println("Dispatcher not configured yet");
        } else {
            dispatcher.register(type.value, HandlerProvider.singleton(frameHandler));
        }
    }

    public void register(Type type, Supplier<FrameHandler> frameHandler ) {
        if (dispatcher == null) {
            System.err.println("Dispatcher not configured yet");
        } else {
            dispatcher.register(type.value, HandlerProvider.factory(frameHandler));
        }
    }
}
