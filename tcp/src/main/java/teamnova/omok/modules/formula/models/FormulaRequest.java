package teamnova.omok.modules.formula.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregates input values for formula execution.
 */
public final class FormulaRequest {
    private final Map<String, Object> values;

    private FormulaRequest(Builder builder) {
        this.values = Collections.unmodifiableMap(new HashMap<>(builder.values));
    }

    public Map<String, Object> values() {
        return values;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, Object> values = new HashMap<>();

        private Builder() { }

        public Builder put(String key, Object value) {
            Objects.requireNonNull(key, "key");
            if (value == null) {
                values.remove(key);
            } else {
                values.put(key, value);
            }
            return this;
        }

        public FormulaRequest build() {
            return new FormulaRequest(this);
        }
    }
}
