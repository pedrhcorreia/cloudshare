package isel.leic.utils;

import jakarta.ws.rs.ForbiddenException;

public class AuthorizationUtils {
    public static void checkAuthorization(Long userId, String authenticatedUsername) {
        if (!authenticatedUsername.equals(String.valueOf(userId))) {
            String errorMessage = String.format("User '%s' is not authorized to access this resource", authenticatedUsername);
            throw new ForbiddenException(errorMessage);
        }
    }
}