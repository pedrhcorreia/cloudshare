package isel.leic.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.common.constraint.NotNull;
import io.smallrye.mutiny.Uni;
import isel.leic.exception.UserNotFoundException;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
    public Uni<Response> getUsers() {
        LOGGER.info("Received get request for all users");
        return userService.findAll()
                .map(users -> {
                    LOGGER.info("HTTP 200 OK: Fetched {} users", users.size());
                    return Response.ok(users).build();
                });
    }

    @PUT
    @Authenticated
    @Path("/{id}")
    public Uni<Response> updateUserPassword(
            @PathParam("id") @NotNull Long id,
            @NotNull String newPassword,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received update password request for user: {}", id);
        return userService.findById(id)
                .onItem().ifNull().failWith(() -> new UserNotFoundException("User with ID " + id + " not found"))
                .onItem().invoke(user -> AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName()))
                .onItem().transformToUni(user -> userService.updatePassword(id, newPassword))
                .map(updatedUser -> {
                    LOGGER.info("HTTP 200 OK: Password updated successfully for user: {}", id);
                    return Response.ok().entity(updatedUser).build();
                });
    }

    @DELETE
    @Authenticated
    @Path("/{id}")
    public Uni<Response> deleteUser(
            @PathParam("id") @NotNull Long id,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received delete request for user: {}", id);
        return userService.findById(id)
                .onItem().ifNull().failWith(() -> new WebApplicationException("User with ID " + id + " not found", Response.Status.NOT_FOUND))
                .onItem().invoke(user -> AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName()))
                .onItem().transformToUni(user -> userService.removeUser(id))
                .onItem().transformToUni(result -> {
                    CompletionStage<String> deleteBucketFuture = minioService.deleteBucket(id + bucket_suffix);
                    return Uni.createFrom().completionStage(deleteBucketFuture);
                })
                .map(deletionResult -> {
                    LOGGER.info("HTTP 200 OK: User {} deleted successfully.", id);
                    return Response.ok().build();
                });
    }


}
