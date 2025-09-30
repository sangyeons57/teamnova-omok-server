package teamnova.omok.handler.register;

import java.util.Objects;
import java.util.function.Supplier;

public interface HandlerProvider {
    FrameHandler acquire();

    static HandlerProvider singleton(FrameHandler handler) {
        Objects.requireNonNull(handler, "handler");
        return () -> handler;
    }

    static HandlerProvider factory(Supplier<FrameHandler> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return supplier::get;
    }
}
