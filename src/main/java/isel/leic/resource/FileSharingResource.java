package isel.leic.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.common.constraint.NotNull;
import isel.leic.exceptions.*;
import isel.leic.model.FileSharing;
import isel.leic.model.Group;
import isel.leic.service.FileSharingService;
import isel.leic.utils.AuthorizationUtils;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Path("/user/{id}/fileshare")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FileSharingResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSharingResource.class);
    @Inject
    FileSharingService fileSharingService;

    @GET
    @Authenticated
    public Response getFilesSharedByUser(
            @PathParam("id") @NotNull Long userId,
            @Context SecurityContext securityContext
    ){

        LOGGER.info("Received request to fetch files shared by user with ID: {}", userId);

        try {
            AuthorizationUtils.checkAuthorization(userId, securityContext.getUserPrincipal().getName());

            List<FileSharing> sharedFiles = fileSharingService.getFilesSharedByUser(userId);
            return Response.ok().entity(sharedFiles).build();
        } catch (UserNotFoundException e) {
            LOGGER.error("HTTP 404 Not Found: User with ID {} not found", userId);
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (FileSharingNotFoundException e) {
            LOGGER.info("HTTP 404 Not Found: No files found for user with ID: {}", userId);
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (ForbiddenException e) {
            LOGGER.error("HTTP 403 Forbidden: User {} is not authorized to fetch files shared by this user",  userId);
            return Response.status(Response.Status.FORBIDDEN).entity("You are not authorized to access this resource").build();
        } catch (Exception e) {
            LOGGER.error("HTTP 500 Internal Server Error: Error while fetching files shared by user with ID: {}", userId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred").build();
        }
    }

    @GET
    @Authenticated
    @Path("/received")
    public Response getFilesSharedToUser(
            @PathParam("id") @NotNull Long sharedToUserId,
            @Context SecurityContext securityContext
    ){

        LOGGER.info("Received request to fetch files shared to user with ID: {} ", sharedToUserId);

        try {
            AuthorizationUtils.checkAuthorization(sharedToUserId, securityContext.getUserPrincipal().getName());

            List<FileSharing> sharedFiles = fileSharingService.getFilesSharedToUser(sharedToUserId);
            return Response.ok().entity(sharedFiles).build();
        } catch (UserNotFoundException e) {
            LOGGER.error("HTTP 404 Not Found: User with ID {} not found", sharedToUserId);
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (FileSharingNotFoundException e) {
            LOGGER.info("HTTP 404 Not Found: No files found shared to user with ID: {}", sharedToUserId);
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (ForbiddenException e) {
            LOGGER.error("HTTP 403 Forbidden: User '{}' is not authorized to fetch files shared to this user",  sharedToUserId);
            return Response.status(Response.Status.FORBIDDEN).entity("You are not authorized to access this resource").build();
        } catch (Exception e) {
            LOGGER.error("HTTP 500 Internal Server Error: Error while fetching files shared to user with ID: {}", sharedToUserId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred").build();
        }
    }

    @POST
    @Authenticated
    public Response shareFiles(             //dont forget TODO find if file exists in the user bucket before attempting to share. also consider saving file type
            @PathParam("id") @NotNull Long userId,
            ShareRequest shareRequest,
            @Context SecurityContext securityContext
    ){
        LOGGER.info("Received request to share files by user with ID: {} )", userId);

        try {
            AuthorizationUtils.checkAuthorization(userId, securityContext.getUserPrincipal().getName());

            if (shareRequest.recipientType == ShareRequest.RecipientType.USER) {
                Long recipientUserId = shareRequest.recipientId;
                String filename = shareRequest.filename;
                FileSharing sharedFile = fileSharingService.shareFileToUser(userId, recipientUserId, filename);
                LOGGER.info("HTTP 201 Created: File '{}' shared successfully from user with ID: {} to user with ID: {}", filename, userId, recipientUserId);
                return Response.ok(sharedFile).build();
            } else if (shareRequest.recipientType == ShareRequest.RecipientType.GROUP) {
                Long recipientGroupId = shareRequest.recipientId;
                String filename = shareRequest.filename;
                List<FileSharing> sharedFile = fileSharingService.shareFileToGroup(userId, recipientGroupId, filename);
                LOGGER.info("HTTP 201 Created: File '{}' shared successfully from user with ID: {} to group with ID: {}", filename, userId, recipientGroupId);
                return Response.ok(sharedFile).build();
            } else {
                LOGGER.error("HTTP 400 Bad Request: Invalid recipient type specified in the request");
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid recipient type specified").build();
            }
        } catch (UserNotFoundException e) {
            LOGGER.error("HTTP 404 Not Found: User not found - {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (GroupNotFoundException e) {
            LOGGER.error("HTTP 404 Not Found: Group not found - {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (DuplicateResourceException e) {
            LOGGER.warn("HTTP 409 Conflict: {}", e.getMessage());
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        } catch(ForbiddenException e) {
            LOGGER.error("HTTP 403 Forbidden: User {} is not authorized to share this file ", userId);
            return Response.status(Response.Status.FORBIDDEN).entity("You are not authorized to access this resource").build();
        } catch(EmptyGroupException e) {
            LOGGER.error("HTTP 404 Not Found: Group is empty - {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (Exception e) {
            LOGGER.error("HTTP 500 Internal Server Error: An error occurred while sharing files - {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred while sharing files").build();
        }
    }

    @DELETE
    @Authenticated
    public Response deleteFileShare(
            @PathParam("id") @NotNull Long userId,
            @Context SecurityContext securityContext,
            @NotNull Long fileShareId
    ) {
        LOGGER.info("Received delete request for file share with ID: {}", fileShareId);

        try {
            AuthorizationUtils.checkAuthorization(userId, securityContext.getUserPrincipal().getName());

            fileSharingService.unshareFile(fileShareId);
            LOGGER.info("HTTP 200 OK: File share {} deleted successfully.", fileShareId);
            return Response.ok().entity("File share " + fileShareId + " deleted successfully.").build();
        } catch (FileSharingNotFoundException e) {
            LOGGER.warn("HTTP 404 Not Found: File share not found - {}", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (ForbiddenException e) {
            LOGGER.error("HTTP 403 Forbidden: User {} is not authorized delete this sharing of a file ", userId);
            return Response.status(Response.Status.FORBIDDEN).entity("You are not authorized to access this resource").build();
        } catch (Exception e) {
            LOGGER.error("HTTP 500 Internal Server Error: An error occurred while deleting file share - {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred while deleting file share").build();
        }
    }


    public record ShareRequest(RecipientType recipientType, Long recipientId, String filename) {

        public enum RecipientType {
            USER,
            GROUP
        }

    }
}

