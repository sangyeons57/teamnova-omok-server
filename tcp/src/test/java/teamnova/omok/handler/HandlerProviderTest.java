package teamnova.omok.handler;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

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
            public void handle(teamnova.omok.nio.NioReactorServer server,
                               teamnova.omok.nio.ClientSession session,
                               teamnova.omok.nio.FramedMessage frame) {
                // no-op
            }
        });
        FrameHandler first = provider.acquire();
        FrameHandler second = provider.acquire();
        assertNotSame(first, second);
    }
}
