package teamnova.omok.handler;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.handler.register.HandlerProvider;
import teamnova.omok.core.nio.ClientSession;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;

class HandlerProviderTest {

    @Test
    void singletonReturnsSameInstance() {
        FrameHandler handler = (server, session, frame) -> { };
        HandlerProvider provider = HandlerProvider.singleton(handler);
        assertSame(handler, provider.acquire());
        assertSame(handler, provider.acquire());
    }

    @Test
    void factoryCreatesNewInstances() {
        HandlerProvider provider = HandlerProvider.factory(() -> new FrameHandler() {
            @Override
            public void handle(NioReactorServer server,
                               ClientSession session,
                               FramedMessage frame) {
                // no-op
            }
        });
        FrameHandler first = provider.acquire();
        FrameHandler second = provider.acquire();
        assertNotSame(first, second);
    }
}
