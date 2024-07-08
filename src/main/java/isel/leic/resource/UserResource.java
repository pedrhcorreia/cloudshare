package isel.leic.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.common.constraint.NotNull;
import isel.leic.model.User;
import isel.leic.service.MinioService;
import isel.leic.service.UserService;
import isel.leic.utils.AuthorizationUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;
    @Inject
    MinioService minioService;

    @ConfigProperty(name = "user.bucket.suffix")
    String bucket_suffix;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserResource.class);

    @GET
    @Authenticated
    public Response getUsers() {
        LOGGER.info("Received get request for all users");
        List<User> users = userService.findAll();
        LOGGER.info("HTTP 200 OK: Fetched {} users", users.size());
        return Response.ok(users).build();
    }
    @GET
    @Path("/{id}")
    @Authenticated
    public Response getUserById(@PathParam("id") @NotNull Long id) {
        LOGGER.info("Received get request for user with id: {}", id);
        User user = userService.findById(id);
        if (user == null) {
            LOGGER.error("User with id: {} not found", id);
            return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
        }
        LOGGER.info("HTTP 200 OK: Fetched user: {} for user id: {}", user.getUsername(), id);
        return Response.ok(user).build();
    }
    @PUT
    @Authenticated
    @Path("/{id}")
    public Response updateUserPassword(
            @PathParam("id") @NotNull Long id,
            @NotNull String newPassword,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received update password request for user: {}", id);
        AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName());
        User updatedUser = userService.updatePassword(id, newPassword);
        LOGGER.info("HTTP 200 OK: Password updated successfully for user: {}", id);
        return Response.ok().entity(updatedUser).build();
    }
    @GET
    @Path("/search")
    @Authenticated
    public Response searchUsers(@QueryParam("prefix") @NotNull String prefix) {
        LOGGER.info("Received search request for users with prefix: {}", prefix);
        Optional<List<User>> optionalUsers = userService.findByUsernamePrefix(prefix);
        List<User> users = Collections.emptyList();
        if (optionalUsers.isPresent()) {
            users = optionalUsers.get();
        }
        LOGGER.info("HTTP 200 OK: Found {} users with prefix: {}", users.size(), prefix);
        return Response.ok(users).build();
    }

    @DELETE
    @Authenticated
    @Path("/{id}")
    public Response deleteUser(
            @PathParam("id") @NotNull Long id,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received delete request for user: {}", id);
        AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName());
        userService.removeUser(id);

        String bucketName = id + bucket_suffix;
        String deleteBucketResult = minioService.deleteBucket(bucketName);
        if (deleteBucketResult.startsWith("Bucket deleted successfully")) {
            LOGGER.info("HTTP 200 OK: User {} deleted successfully.", id);
            return Response.ok().build();
        } else {
            LOGGER.error("Error deleting bucket for user {}: {}", id, deleteBucketResult);
            return Response.serverError().entity(deleteBucketResult).build();
        }
    }



}
