package isel.leic.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import isel.leic.exception.TokenException;
import jakarta.ws.rs.ForbiddenException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

public class AnonymousAccessUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnonymousAccessUtils.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256"; // Using HMAC with SHA-256

    private static final String SECRET_KEY = "5g8PZn@^u8#vQwYs2Tc$L6rHm!eFjXpA";
    public static String encodeToken(long expiration, Long sharedByUserId, String fileName) {
        try {
            // Create a JSON object containing the parameters
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(Map.of(
                    "expiration", expiration,
                    "sharedByUserId", sharedByUserId,
                    "fileName", fileName
            ));

            // Encode the JSON string using Base64 encoding
            String base64EncodedJson = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

            // Calculate HMAC of the Base64-encoded JSON string using SHA-256 and the secret key
            Mac hmacSha256 = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            hmacSha256.init(secretKey);
            byte[] hmacBytes = hmacSha256.doFinal(base64EncodedJson.getBytes(StandardCharsets.UTF_8));

            // Encode the HMAC bytes using Base64 encoding
            String encodedHmac = Base64.getEncoder().encodeToString(hmacBytes);

            // Construct the token by concatenating the Base64-encoded JSON string and the HMAC
            String token = base64EncodedJson + "." + encodedHmac;

            LOGGER.info("Token encoded successfully.");
            return token;
        } catch (NoSuchAlgorithmException | InvalidKeyException | JsonProcessingException e) {
            LOGGER.error("Error encoding token: {}", e.getMessage());
            throw new TokenException(e.getMessage());
        }
    }

    public static Map<String, Object> decodeToken(String token) {
        try {
            String[] parts = token.split("\\.");
            String base64EncodedJson = parts[0];
            String encodedHmac = parts[1];

            // Decode the Base64-encoded JSON string
            byte[] jsonBytes = Base64.getDecoder().decode(base64EncodedJson);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);

            // Parse the JSON string into a Map
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> decodedMap = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});

            // Extract expiration time from the decoded token
            // Extract expiration time from the decoded token
            Object expirationObj = decodedMap.get("expiration");
            if (expirationObj instanceof Integer) {
                expirationObj = ((Integer) expirationObj).longValue(); // Convert Integer to Long
            }
            long expiration = (Long) expirationObj; // Now expiration is guaranteed to be of type Long

            long currentTimeMillis = System.currentTimeMillis();

            // Check if the token is expired
            if (expiration < currentTimeMillis / 1000) {
                LOGGER.error("Token has expired.");
                throw new TokenException("Token has expired.");
            }

            // Calculate HMAC of the Base64-encoded JSON string using SHA-256 and the secret key
            Mac hmacSha256 = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            hmacSha256.init(secretKey);
            byte[] hmacBytes = hmacSha256.doFinal(base64EncodedJson.getBytes(StandardCharsets.UTF_8));
            String calculatedHmac = Base64.getEncoder().encodeToString(hmacBytes);

            // Verify that the calculated HMAC matches the encoded HMAC
            if (!calculatedHmac.equals(encodedHmac)) {
                LOGGER.error("Token integrity check failed.");
                throw new ForbiddenException("Invalid link.");
            }

            LOGGER.info("Token decoded successfully.");
            return decodedMap;
        } catch (NoSuchAlgorithmException | InvalidKeyException | JsonProcessingException e) {
            LOGGER.error("Error decoding token: {}", e.getMessage());
            throw new TokenException(e.getMessage());
        }
    }



}
