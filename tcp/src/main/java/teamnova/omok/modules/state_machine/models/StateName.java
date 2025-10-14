package teamnova.omok.modules.state_machine.models;

public record StateName(String name) {
    public static StateName of(String name) {
        return new StateName(name);
    }

    public static StateName defaultState() {
        return new StateName("default state");
    }
}
