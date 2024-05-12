package isel.leic.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.common.constraint.NotNull;
import isel.leic.model.storage.FileObject;
import isel.leic.model.storage.FormData;
import isel.leic.service.FileSharingService;
import isel.leic.service.MinioService;
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

@Path("/user/{id}/object")
public class MinioResource {

    @Inject
    MinioService minioService;
    @Inject
    FileSharingService fileSharingService;
    @ConfigProperty(name = "user.bucket.suffix")
    String bucket_suffix;

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioResource.class);


    @POST
    @Authenticated
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(
            @NotNull FormData formData,
            @PathParam("id") @NotNull Long id,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to upload file for user with ID: {}", id);
        authorize(id, securityContext);
        String userId = getUserId(securityContext);
        String bucketName = userId + bucket_suffix;
        CompletableFuture<String> uploadFuture = minioService.uploadObject(bucketName, formData);
        return uploadFuture.thenApply(response -> {
            if (response.startsWith("Object uploaded successfully")) {
                LOGGER.info("File uploaded successfully for user with ID: {}", id);
                return Response.ok().status(Response.Status.CREATED).build();
            } else {
                LOGGER.error("Error uploading file for user with ID: {}. Error: {}", id, response);
                return Response.serverError().entity(response).build();
            }
        }).join();
    }

    @GET
    @Path("/{objectKey}")
    @Authenticated
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(
            @PathParam("id") @NotNull Long id,
            @PathParam("objectKey") @NotNull String objectKey,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to download file '{}' for user with ID: {}", objectKey, id);
        try {
            authorize(id, securityContext);
        } catch (ForbiddenException e) {
            //check if file was shared with this user
            String userId = getUserId(securityContext);
            if (!fileSharingService.isFileSharedWithUser(id, Long.valueOf(userId), objectKey)) {
                //File is not shared with this user
                String errorMessage = String.format("User '%s' is not authorized to access this resource", id);
                LOGGER.error(errorMessage);
                throw new ForbiddenException(errorMessage);
            } else {
                LOGGER.info("User '{}' is accessing file '{}' shared by user '{}'", userId, objectKey, id);
            }
        }
        String bucketName = id + bucket_suffix; // Assuming bucket_suffix is properly configured
        CompletableFuture<byte[]> objectBytes = minioService.downloadObject(bucketName, objectKey);
        if (objectBytes.join() != null) {
            LOGGER.info("File '{}' downloaded successfully for user with ID: {}", objectKey, id);
            Response.ResponseBuilder response = Response.ok(objectBytes.join());
            response.header("Content-Disposition", "attachment;filename=" + objectKey);
            return response.build();
        } else {
            LOGGER.error("File '{}' not found for user with ID: {}", objectKey, id);
            return Response.status(Response.Status.NOT_FOUND).entity("File not found").build();
        }
    }

    @GET
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public List<FileObject> listFiles(
            @PathParam("id") @NotNull Long id,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to list files for user with ID: {}", id);
        authorize(id, securityContext);
        String bucketName = id + bucket_suffix;
        CompletableFuture<List<FileObject>> files = minioService.listObjects(bucketName, null);
        if (files != null) {
            LOGGER.info("Files listed successfully for user with ID: {}", id);
            return files.join();
        } else {
            LOGGER.error("Error listing files for user with ID: {}", id);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @DELETE
    @Path("/{objectKey}")
    @Authenticated
    public Response deleteFile(
            @PathParam("id") @NotNull Long userId,
            @PathParam("objectKey") @NotNull String objectKey,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to delete file '{}' for user with ID: {}", objectKey, userId);
        authorize(userId, securityContext);
        String bucketName = userId + bucket_suffix; // Assuming bucket_suffix is properly configured
        CompletableFuture<String> response = minioService.deleteObject(bucketName, objectKey);
        if (response.join().startsWith("Object deleted successfully")) {
            LOGGER.info("File '{}' deleted successfully for user with ID: {}", objectKey, userId);
            return Response.ok().build();
        } else {
            LOGGER.error("Error deleting file '{}' for user with ID: {}. Error: {}", objectKey, userId, response);
            return Response.serverError().entity(response).build();
        }
    }

    @PUT
    @Path("/{objectKey}")
    @Authenticated
    public Response renameFile(
            @PathParam("id") @NotNull Long userId,
            @PathParam("objectKey") @NotNull String objectKey,
            @QueryParam("newName") @NotNull String newName,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to rename file '{}' for user with ID: {}", objectKey, userId);
        authorize(userId, securityContext);
        String bucketName = userId + bucket_suffix; // Assuming bucket_suffix is properly configured
        CompletableFuture<String> response = minioService.renameObject(bucketName, objectKey, newName);
        if (response.join().startsWith("Object renamed successfully")) {
            LOGGER.info("File '{}' renamed successfully for user with ID: {}", objectKey, userId);
            return Response.ok().build();
        } else {
            LOGGER.error("Error renaming file '{}' for user with ID: {}. Error: {}", objectKey, userId, response);
            return Response.serverError().entity(response).build();
        }
    }

    // Utility method to authorize user
    private void authorize(Long userId, SecurityContext securityContext) {
        AuthorizationUtils.checkAuthorization(userId, securityContext.getUserPrincipal().getName());
    }

    // Utility method to extract userId from SecurityContext
    private String getUserId(SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }
}
