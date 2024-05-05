package isel.leic.resource;

import io.quarkus.security.Authenticated;
import isel.leic.model.User;
import isel.leic.service.MinioService;
import isel.leic.service.UserService;
import isel.leic.utils.TokenUtils;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;


@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthenticationResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationResource.class);


    @Inject
    UserService userService;
    @Inject
    MinioService minioService;

    @ConfigProperty(name = "com.cloudshare.quarkusjwt.jwt.duration")
    Long tokenDuration;

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String tokenIssuer;

    @ConfigProperty(name = "user.bucket.suffix")
    String bucket_suffix;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9-]{3,20}$");

    @POST
    @PermitAll
    @Path("/login")
    public Response login(LoginRequest loginRequest) {
        LOGGER.info("Received login request for user: {}", loginRequest.username);
        User user = userService.authenticate(loginRequest.username, loginRequest.password).orElse(null);
        if (user != null) {
            LOGGER.info("User authenticated successfully: {}", user.getUsername());
            try {
                String token = TokenUtils.generateToken(user.getId(), tokenIssuer, tokenDuration);
                LOGGER.info("Generated token for user: {}", user.getUsername());
                return Response.ok().entity(token).build();
            } catch (Exception e) {
                LOGGER.error("Failed to generate token for user: {}", user.getUsername(), e);
                // Handle this error properly TODO
                return Response.serverError().build();
            }
        } else {
            LOGGER.warn("Authentication failed for user: {}", loginRequest.username);
            // If authentication fails
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
        }
    }

    @POST
    @PermitAll
    @Path("/signup")
    public Response signup(LoginRequest signupRequest) {
        LOGGER.info("Received signup request for user: {}", signupRequest.username);
        if (!isValidUsername(signupRequest.username)) {
            LOGGER.warn("Invalid username format for user: {}", signupRequest.username);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid username format. Username must contain only letters and numbers, between 3 and 20 characters.")
                    .build();
        }
        if (userService.findByUsername(signupRequest.username) != null) {
            LOGGER.warn("Username already exists: {}", signupRequest.username);
            return Response.status(Response.Status.CONFLICT)
                    .entity("Username already exists")
                    .build();
        }
        User newUser = new User(signupRequest.username, signupRequest.password);
        userService.createUser(newUser);
        minioService.createBucket(newUser.getId()+bucket_suffix);
        try {
            String token = TokenUtils.generateToken(newUser.getId(), tokenIssuer, tokenDuration);
            LOGGER.info("User signed up successfully: {}", newUser.getUsername());
            return Response.ok().entity(token).build();
        } catch (Exception e) {
            LOGGER.error("Failed to generate token for user: {}", newUser.getUsername(), e);
            // Handle token generation error TODO
            return Response.serverError().build();
        }
    }

    @DELETE
    @Authenticated
    @Path("/{id}")
    public Response deleteUser(@PathParam("id") Long id, @Context SecurityContext securityContext) {
        LOGGER.info("Received delete request for user: {}", id);
        String authenticatedUsername = securityContext.getUserPrincipal().getName();

        if (!authenticatedUsername.equals(String.valueOf(id))) {
            LOGGER.warn("Unauthorized attempt to delete user: {}", id);
            return Response.status(Response.Status.UNAUTHORIZED).entity("You are not authorized to delete this user").build();
        }
        User user = userService.findById(id);

        if (user == null) {
            LOGGER.warn("User not found: {}", id);
            return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
        }

        userService.delete(user);
        minioService.deleteBucket(id + bucket_suffix);

        LOGGER.info("User {} deleted successfully.", id);
        return Response.ok().entity("User " + id + " deleted successfully.").build();
    }

    @POST
    @Path("/refresh-token")
    @Authenticated
    public Response refreshToken(@Context SecurityContext securityContext) {
        String userId = securityContext.getUserPrincipal().getName();
        LOGGER.info("Received refresh token request for user: {}", userId);
        try {
            String newToken = TokenUtils.generateToken(Long.valueOf(userId), tokenIssuer, tokenDuration);
            return Response.ok(newToken).build();
        } catch (Exception e) {
            LOGGER.error("Failed to refresh token for user: {}", userId, e);
            return Response.status(Response.Status.UNAUTHORIZED).entity("Token refresh failed").build();
        }
    }


    public static class LoginRequest {
        public String username;
        public String password;

    }

    private boolean isValidUsername(String username) {
        return USERNAME_PATTERN.matcher(username).matches();
    }
}
