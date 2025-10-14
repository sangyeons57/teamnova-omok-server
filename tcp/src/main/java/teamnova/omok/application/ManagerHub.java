package teamnova.omok.application;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class ManagerHub {
    private final Set<Manager> managers = new HashSet<>();

    public void registerManager(Manager manager) {
        managers.add(manager);
        manager.start();
    }

    public void process() {
        for(Manager manager : managers) {
            manager.update();
        }
    }

    public void unregisterManager(Manager manager) {
        manager.stop();
        managers.remove(manager);
    }
}
