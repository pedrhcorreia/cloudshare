package isel.leic.unit;

import io.quarkus.test.junit.QuarkusTest;
import isel.leic.utils.AnonymousAccessUtils;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AnonymousAccessUtilsTest {

    private static String token;
    private static long expiration;
    @Test
    @Order(1)
    void testEncodeToken() {
        // Mock data
        expiration = System.currentTimeMillis() + 3600_000; // 1 hour from now
        Long sharedByUserId = 456L;
        String fileName = "example.txt";

        token = AnonymousAccessUtils.encodeToken(expiration, sharedByUserId, fileName);

        assertNotNull(token);
    }

    @Test
    @Order(2)
    void testDecodeToken() {
        Map<String, Object> decodedToken = AnonymousAccessUtils.decodeToken(token);

        assertEquals(expiration, decodedToken.get("expiration"));
        assertEquals(456, decodedToken.get("sharedByUserId"));
        assertEquals("example.txt", decodedToken.get("fileName"));
    }
}
