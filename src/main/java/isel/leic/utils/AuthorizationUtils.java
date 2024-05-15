package isel.leic.utils;

import jakarta.ws.rs.ForbiddenException;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationUtils.class);

    public static void checkAuthorization(Long userId, String authenticatedUsername) {
        if (!authenticatedUsername.equals(String.valueOf(userId))) {
            String errorMessage = String.format("User '%s' is not authorized to access this resource", authenticatedUsername);
            throw new ForbiddenException(errorMessage);
        } else {
            LOGGER.info("User '{}' is authorized to access this resource", authenticatedUsername);
        }
    }

    public static String encodePassword(String password) {
        String salt = BCrypt.gensalt();
        return BCrypt.hashpw(password, salt);
    }

    public static boolean verifyPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}