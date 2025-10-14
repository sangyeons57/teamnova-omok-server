package teamnova.omok.infra;

public class InfraContainer {
    private final Dotenv defaultEnv;
    private final Mysql defaultDB;

    public InfraContainer() {
        this.defaultEnv = new Dotenv(System.getProperty("user.dir") + "/..");
        this.defaultDB = new Mysql(this.defaultEnv);
    }

    public Dotenv getDefaultEnv() {
        return this.defaultEnv;
    }

    public Mysql getDefaultDB() {
        return defaultDB;
    }
}
