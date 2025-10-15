package teamnova.omok.modules.formula.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable parameter snapshot shared across formula steps.
 */
public final class FormulaParams {
    private final Map<String, Object> data;

    private FormulaParams(Map<String, Object> data) {
        this.data = Collections.unmodifiableMap(data);
    }

    public static FormulaParams of(Map<String, Object> data) {
        Objects.requireNonNull(data, "data");
        return new FormulaParams(new HashMap<>(data));
    }

    public Map<String, Object> data() {
        return data;
    }

    public boolean contains(String key) {
        return data.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public double getDouble(String key, double defaultValue) {
        Object raw = data.get(key);
        if (raw == null) {
            return defaultValue;
        }
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        throw new ClassCastException("Value " + key + " is not numeric");
    }

    public int getInt(String key, int defaultValue) {
        Object raw = data.get(key);
        if (raw == null) {
            return defaultValue;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        throw new ClassCastException("Value " + key + " is not numeric");
    }

    public FormulaParams with(String key, Object value) {
        Map<String, Object> updated = new HashMap<>(data);
        if (value == null) {
            updated.remove(key);
        } else {
            updated.put(key, value);
        }
        return new FormulaParams(updated);
    }

    public FormulaParams withAll(Map<String, Object> entries) {
        Objects.requireNonNull(entries, "entries");
        Map<String, Object> updated = new HashMap<>(data);
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            if (entry.getValue() == null) {
                updated.remove(entry.getKey());
            } else {
                updated.put(entry.getKey(), entry.getValue());
            }
        }
        return new FormulaParams(updated);
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
