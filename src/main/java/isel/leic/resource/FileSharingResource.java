package isel.leic.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.common.constraint.NotNull;
import isel.leic.model.FileSharing;
import isel.leic.model.FileSharingResponse;
import isel.leic.service.FileSharingService;
import isel.leic.utils.AuthorizationUtils;
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

        AuthorizationUtils.checkAuthorization(userId, securityContext.getUserPrincipal().getName());

        List<FileSharingResponse> sharedFiles = fileSharingService.getFilesSharedByUser(userId);
        LOGGER.info("HTTP 200 OK: Files shared by user with ID {} fetched successfully", userId);
        return Response.ok().entity(sharedFiles).build();
    }

    @GET
    @Authenticated
    @Path("/received")
    public Response getFilesSharedToUser(
            @PathParam("id") @NotNull Long sharedToUserId,
            @Context SecurityContext securityContext
    ){

        LOGGER.info("Received request to fetch files shared to user with ID: {} ", sharedToUserId);
        AuthorizationUtils.checkAuthorization(sharedToUserId, securityContext.getUserPrincipal().getName());
        List<FileSharingResponse> sharedFiles = fileSharingService.getFilesSharedToUser(sharedToUserId);
        LOGGER.info("HTTP 200 OK: Files shared to user with ID {} fetched successfully", sharedToUserId);
        return Response.ok().entity(sharedFiles).build();
    }

    @POST
    @Authenticated
    public Response shareFiles(
            @PathParam("id") @NotNull Long userId,
            @NotNull ShareRequest shareRequest,
            @Context SecurityContext securityContext
    ){
        LOGGER.info("Received request to share files by user with ID: {} )", userId);

        AuthorizationUtils.checkAuthorization(userId, securityContext.getUserPrincipal().getName());
        if (shareRequest.recipientType == ShareRequest.RecipientType.USER) {
            Long recipientUserId = shareRequest.recipientId;
            String filename = shareRequest.filename;
            FileSharing sharedFile = fileSharingService.shareFileToUser(userId, recipientUserId, filename);
            LOGGER.info("HTTP 201 Created: File '{}' shared successfully from user with ID: {} to user with ID: {}", filename, userId, recipientUserId);
            return Response.ok(sharedFile).build();
        } else {
            Long recipientGroupId = shareRequest.recipientId;
            String filename = shareRequest.filename;
            List<FileSharing> sharedFile = fileSharingService.shareFileToGroup(userId, recipientGroupId, filename);
            LOGGER.info("HTTP 201 Created: File '{}' shared successfully from user with ID: {} to group with ID: {}", filename, userId, recipientGroupId);
            return Response.ok(sharedFile).build();
        }
    }

    @DELETE
    @Authenticated
    public Response deleteFileShare(
            @PathParam("id") @NotNull Long userId,
            @Context SecurityContext securityContext,
            @NotNull deleteShareRequest fileShareId
    ) {
        LOGGER.info("Received delete request for file share with ID: {}", fileShareId);

        AuthorizationUtils.checkAuthorization(userId, securityContext.getUserPrincipal().getName());

        fileSharingService.unshareFile(fileShareId.fileShareId);
        LOGGER.info("HTTP 200 OK: File share {} deleted successfully.", fileShareId);
        return Response.ok().build();
    }

    public record ShareRequest(RecipientType recipientType, Long recipientId, String filename) {

        public enum RecipientType {
            USER,
            GROUP
        }

    }
    public record deleteShareRequest(Long fileShareId){

    }
}
