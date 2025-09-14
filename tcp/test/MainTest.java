import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class MainTest {

    @Test
    void helloWorldIsPrinted() throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(outContent));
            Main.main(new String[]{});
        } finally {
            System.setOut(originalOut);
        }
        String output = outContent.toString().replace("\r\n", "\n").trim();
        assertEquals("Hello World", output);
    }

    @Test
    void jacksonSerializationWorks() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> data = new HashMap<>();
        data.put("message", "hi");
        data.put("value", 42);
        String json = mapper.writeValueAsString(data);
        assertTrue(json.contains("\"message\":\"hi\""));
        assertTrue(json.contains("\"value\":42"));
    }
}
