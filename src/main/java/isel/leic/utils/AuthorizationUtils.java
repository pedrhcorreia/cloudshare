package isel.leic.utils;

import jakarta.ws.rs.ForbiddenException;
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
}