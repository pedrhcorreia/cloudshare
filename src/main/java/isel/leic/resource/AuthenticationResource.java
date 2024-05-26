package isel.leic.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.common.constraint.NotNull;
import jakarta.json.Json;
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

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;


@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthenticationResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationResource.class);
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9-]{3,20}$");
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



    @POST
    @PermitAll
    @Path("/login")
    public Response login(@NotNull LoginRequest loginRequest) throws Exception {
        LOGGER.info("Received login request for user: {}", loginRequest.username);
        User user = userService.authenticate(loginRequest.username, loginRequest.password);

        LOGGER.info("HTTP 200 OK: User authenticated successfully: {}", user.getUsername());
        String token = TokenUtils.generateToken(user.getId(), tokenIssuer, tokenDuration);
        LOGGER.info("Generated token for user: {}", user.getUsername());


        LOGGER.info("HTTP 200 OK: User authenticated successfully: {}", user.getUsername());
        return Response.ok(userAndTokenJson(user,token)).build();
    }


    @POST
    @PermitAll
    @Path("/signup")
    public Response signup(@NotNull LoginRequest signupRequest) throws Exception {
        LOGGER.info("Received signup request for user: {}", signupRequest.username);
        if (!isValidUsername(signupRequest.username)) {
            LOGGER.warn("HTTP 400 Bad Request: Invalid username format for user: {}", signupRequest.username);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid username format. Username must contain only letters and numbers, between 3 and 20 characters.")
                    .build();
        }
        User newUser = new User(signupRequest.username, signupRequest.password);
        userService.createUser(newUser);
        CompletableFuture<String> createBucketFuture = minioService.createBucket(newUser.getId() + bucket_suffix);
        // Await the completion of bucket creation
        createBucketFuture.join(); // This will block until the bucket creation completes
        String token = TokenUtils.generateToken(newUser.getId(), tokenIssuer, tokenDuration);
        LOGGER.info("HTTP 200 OK: User signed up successfully: {}", newUser.getUsername());
        return Response.ok(userAndTokenJson(newUser, token)).build();
    }

    @POST
    @Path("/refresh-token")
    @Authenticated
    public Response refreshToken(@Context SecurityContext securityContext) throws Exception {
        String userId = securityContext.getUserPrincipal().getName();
        LOGGER.info("Received refresh token request for user: {}", userId);
        String newToken = TokenUtils.generateToken(Long.valueOf(userId), tokenIssuer, tokenDuration);
        LOGGER.info("HTTP 200 OK: Token refreshed successfully for user: {}", userId);
        return Response.ok(Json.createObjectBuilder().add("token", newToken).build()).build();
    }



    public record LoginRequest(String username, String password) {

    }

    private boolean isValidUsername(String username) {
        return USERNAME_PATTERN.matcher(username).matches();
    }

    private String userAndTokenJson(User user, String token) {
        return "{ \"token\": \"" + token + "\", \"user\": { \"id\": " + user.getId() + ", \"username\": \"" + user.getUsername() + "\" }}";
    }


}
