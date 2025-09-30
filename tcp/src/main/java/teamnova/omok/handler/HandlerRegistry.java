package teamnova.omok.handler;

import java.util.Objects;
import java.util.function.Consumer;
import teamnova.omok.dispatcher.Dispatcher;

public interface HandlerRegistry {
    void configure(Dispatcher dispatcher);

    static HandlerRegistry empty() {
        return dispatcher -> { };
    }

    static HandlerRegistry of(Consumer<Dispatcher> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        return consumer::accept;
    }

    static HandlerRegistry compose(HandlerRegistry... registries) {
        Objects.requireNonNull(registries, "registries");
        return dispatcher -> {
            for (HandlerRegistry registry : registries) {
                if (registry == null) {
                    continue;
                }
                registry.configure(dispatcher);
            }
        };
    }
}
