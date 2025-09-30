package teamnova.omok.handler;

import teamnova.omok.codec.decoder.HelloWorldDecoder;
import teamnova.omok.dispatcher.Dispatcher;

/**
 * Registers built-in handlers used by the server for testing/demo purposes.
 */
public final class DefaultHandlerRegistry implements HandlerRegistry {
    private final HelloWorldDecoder helloWorldDecoder;

    public DefaultHandlerRegistry() {
        this(new HelloWorldDecoder());
    }

    public DefaultHandlerRegistry(HelloWorldDecoder helloWorldDecoder) {
        this.helloWorldDecoder = helloWorldDecoder;
    }

    @Override
    public void configure(Dispatcher dispatcher) {
        dispatcher.register(0, HandlerProvider.singleton(new HelloWorldHandler(helloWorldDecoder)));
    }
}
