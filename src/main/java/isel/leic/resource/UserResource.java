package isel.leic.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.common.constraint.NotNull;
import isel.leic.model.User;
import isel.leic.service.MinioService;
import isel.leic.service.UserService;
import isel.leic.utils.AuthorizationUtils;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
        minioService.deleteBucket(id + bucket_suffix);
        LOGGER.info("HTTP 200 OK: User {} deleted successfully.", id);
        return Response.ok().build();
    }



}
