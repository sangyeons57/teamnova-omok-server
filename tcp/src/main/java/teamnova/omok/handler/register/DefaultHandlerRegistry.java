package teamnova.omok.handler.register;

import teamnova.omok.handler.AuthHandler;
import teamnova.omok.handler.PingPongHandler;
import teamnova.omok.message.decoder.HelloWorldDecoder;
import teamnova.omok.handler.dispatcher.Dispatcher;
import teamnova.omok.handler.HelloWorldHandler;
import teamnova.omok.message.decoder.StringDecoder;
import teamnova.omok.handler.JoinMatchHandler;
import teamnova.omok.handler.LeaveInGameSessionHandler;
import teamnova.omok.handler.PlaceStoneHandler;
import teamnova.omok.handler.ReadyInGameSessionHandler;
import teamnova.omok.service.ServiceContainer;

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
        register(Type.LEAVE_IN_GAME_SESSION, new LeaveInGameSessionHandler());
        register(Type.READY_IN_GAME_SESSION, new ReadyInGameSessionHandler());
        register(Type.PLACE_STONE, new PlaceStoneHandler(stringDecoder));
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
