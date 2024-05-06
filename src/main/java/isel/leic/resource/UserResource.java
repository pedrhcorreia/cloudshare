package isel.leic.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.common.constraint.NotNull;
import isel.leic.exceptions.*;
import isel.leic.model.User;
import isel.leic.service.UserService;
import isel.leic.utils.AuthorizationUtils;
import jakarta.annotation.Priority;
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

@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    @ConfigProperty(name = "user.bucket.suffix")
    String bucket_suffix;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserResource.class);

    @GET
    @Authenticated
    public Response getUsers(){
        LOGGER.info("Received get request for all users");
        try {
            List<User> users = userService.findAll();
            if (users.isEmpty()){
                LOGGER.info("HTTP 404 Not Found: No users found");
                return Response.status(Response.Status.NOT_FOUND).entity("No users found").build();
            }
            return Response.ok(users).build();
        } catch (Exception e) {
            LOGGER.error("HTTP 500 Internal Server Error: An error occurred while fetching all users - {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
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

        try {
            AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName());
            userService.updatePassword(id, newPassword);
            LOGGER.info("HTTP 200 OK: Password updated successfully for user: {}", id);
            return Response.ok().entity("Password updated successfully for user " + id).build();
        } catch (UserNotFoundException e) {
            LOGGER.warn("HTTP 404 Not Found: User not found - {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (ForbiddenException e){
            LOGGER.warn("HTTP 403 Unauthorized: Unauthorized attempt to update password for user: {}", id);
            return Response.status(Response.Status.FORBIDDEN).entity("You are not authorized to update the password for this user").build();
        } catch (Exception e) {
            LOGGER.error("HTTP 500 Internal Server Error: An error occurred while updating password for user - {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred while updating password for user").build();
        }
    }

    @DELETE
    @Authenticated
    @Path("/{id}")
    public Response deleteUser(
            @PathParam("id") @NotNull Long id,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received delete request for user: {}", id);
        try {
            AuthorizationUtils.checkAuthorization(id, securityContext.getUserPrincipal().getName());
            userService.removeUser(id);
            //minioService.deleteBucket(id + bucket_suffix);
            //TODO check for errors in the minioService
            LOGGER.info("HTTP 200 OK: User {} deleted successfully.", id);
            return Response.ok().entity("User " + id + " deleted successfully.").build();
        } catch (UserNotFoundException e) {
            LOGGER.warn("HTTP 404 Not Found: User not found - {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (ForbiddenException e) {
            LOGGER.warn("HTTP 403 Unauthorized: Unauthorized attempt to delete user: {}", id);
            return Response.status(Response.Status.FORBIDDEN).entity("You are not authorized to delete this user").build();
        }catch (Exception e) {
            LOGGER.error("HTTP 500 Internal Server Error: An error occurred while deleting user - {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred while deleting user").build();
        }
    }



}
