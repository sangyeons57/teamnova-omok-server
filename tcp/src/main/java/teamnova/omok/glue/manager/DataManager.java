package teamnova.omok.glue.manager;

import teamnova.omok.glue.data.DotenvService;
import teamnova.omok.glue.data.MysqlService;
import teamnova.omok.glue.data.model.UserData;
import teamnova.omok.glue.data.model.UserScoreData;

public class DataManager {
    private static DataManager instance;

    public static DataManager Init() {
        instance = new DataManager();
        return instance;
    }

    public static DataManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DataManager not initialized");
        }
        return instance;
    }

    private final DotenvService dotenvService;
    private final MysqlService mysqlService;

    private DataManager() {
        String basePath = System.getProperty("user.dir") + "/..";
        this.dotenvService = new DotenvService(basePath);
        this.mysqlService = new MysqlService(dotenvService);
    }

    public String getFromDotEnv(String key) {
        return dotenvService.get(key);
    }

    public static UserData getDefaultUser() {
        return new UserData("", "", 0, UserData.Status.INACTIVE, 0);
    }

    public UserData findUser(String userId, UserData defaultUserData) {
        return mysqlService.findUser(userId, defaultUserData);
    }

    public UserScoreData getUserScore(String userId, int defaultScore) {
        return getUserScore(userId, UserScoreData.of(defaultScore));
    }
    public UserScoreData getUserScore(String userId, UserScoreData defaultScore) {
        return mysqlService.getUserScore(userId, defaultScore);
    }
    public boolean adjustUserScore(String userId, int delta) {
        return mysqlService.adjustUserScore(userId, delta);
    }
}
