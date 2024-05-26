package isel.leic.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.common.constraint.NotNull;
import io.smallrye.mutiny.Uni;
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
import java.util.concurrent.CompletionStage;
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
    public Uni<Response> login(@NotNull LoginRequest loginRequest) {
        LOGGER.info("Received login request for user: {}", loginRequest.username);
        return Uni.createFrom().item(loginRequest)
                .onItem().transformToUni(request ->
                        userService.authenticate(request.username, request.password)
                                .map(user -> {
                                    LOGGER.info("HTTP 200 OK: User authenticated successfully: {}", user.getUsername());
                                    String token;
                                    try {
                                        token = TokenUtils.generateToken(user.getId(), tokenIssuer, tokenDuration);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    LOGGER.info("Generated token for user: {}", user.getUsername());
                                    return Response.ok(userAndTokenJson(user, token)).build();
                                })
                                .onFailure().recoverWithItem(error -> {
                                    LOGGER.error("HTTP 401 Unauthorized: Authentication failed for user: {}", request.username);
                                    return Response.status(Response.Status.UNAUTHORIZED).build();
                                })
                );
    }



    @POST
    @PermitAll
    @Path("/signup")
    public Uni<Response> signup(@NotNull LoginRequest signupRequest) {
        LOGGER.info("Received signup request for user: {}", signupRequest.username);
        if (!isValidUsername(signupRequest.username)) {
            LOGGER.warn("HTTP 400 Bad Request: Invalid username format for user: {}", signupRequest.username);
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid username format. Username must contain only letters and numbers, between 3 and 20 characters.")
                    .build());
        }

        User newUser = new User(signupRequest.username, signupRequest.password);
        return userService.createUser(newUser)
                .flatMap(createdUser -> {
                    CompletionStage<String> createBucketFuture = minioService.createBucket(createdUser.getId() + bucket_suffix);
                    return Uni.createFrom().completionStage(createBucketFuture)
                            .map(bucketCreationResponse -> {
                                if (bucketCreationResponse.startsWith("Bucket created successfully")) {
                                    String token = null;
                                    try {
                                        token = TokenUtils.generateToken(createdUser.getId(), tokenIssuer, tokenDuration);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    LOGGER.info("HTTP 200 OK: User signed up successfully: {}", createdUser.getUsername());
                                    return Response.ok(userAndTokenJson(createdUser, token)).build();
                                } else {
                                    LOGGER.error("HTTP 500 Internal Server Error: Error creating bucket for user: {}", createdUser.getUsername());
                                    return Response.serverError().entity("Error creating bucket").build();
                                }
                            });
                });
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
