package teamnova.omok.infra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Loads environment variables from a .env file located at the given base path.
 */
public class Dotenv {
    private static final String ENV_FILE = ".env";
    private final Map<String, String> env;

    public Dotenv(String basePath) {
        Path path = Paths.get(basePath, ENV_FILE);
        System.out.println("Loading environment variables from " + path);
        Map<String, String> envMap;
        try {
            envMap = Files.lines(path)
                    .filter(l -> !l.trim().startsWith("#") && !l.trim().isEmpty())
                    .map(l -> l.split("=",2))
                    .collect(
                            Collectors.toMap(
                                    a -> a[0].trim(),
                                    a -> a.length == 2 ? a[1].trim().replaceAll("^\"|\"$", "") : "")
                    );
        } catch (IOException e) {
            System.err.println("Failed to read .env file: " + e.getMessage());
            envMap = new HashMap<>();
        }
        this.env = envMap;
    }

    public String get(String key) {
        return env.get(key);
    }
}
