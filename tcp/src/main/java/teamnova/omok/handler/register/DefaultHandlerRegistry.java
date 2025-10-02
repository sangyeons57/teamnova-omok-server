package teamnova.omok.handler.register;

import teamnova.omok.handler.AuthHandler;
import teamnova.omok.handler.PingPongHandler;
import teamnova.omok.handler.decoder.HelloWorldDecoder;
import teamnova.omok.dispatcher.Dispatcher;
import teamnova.omok.handler.HelloWorldHandler;
import teamnova.omok.handler.decoder.StringDecoder;
import teamnova.omok.handler.service.DotenvService;

/**
 * Registers built-in handlers used by the server for testing/demo purposes.
 */
public final class DefaultHandlerRegistry implements HandlerRegistry {
    private final HelloWorldDecoder helloWorldDecoder;
    private final StringDecoder stringDecoder;

    private final DotenvService dotenvService;

    public DefaultHandlerRegistry() {
        this.helloWorldDecoder = new HelloWorldDecoder();
        this.stringDecoder = new StringDecoder();

        this.dotenvService = new DotenvService(System.getProperty("user.dir") + "/../" );
    }

    @Override
    public void configure(Dispatcher dispatcher) {
        dispatcher.register(0, HandlerProvider.singleton(new HelloWorldHandler(helloWorldDecoder)));
        dispatcher.register(1, HandlerProvider.singleton(new AuthHandler(stringDecoder, dotenvService)));
        dispatcher.register(2, HandlerProvider.singleton(new PingPongHandler()));
    }
}
