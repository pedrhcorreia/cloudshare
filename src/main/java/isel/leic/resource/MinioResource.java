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

import java.net.URL;
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
    @Path("/presign/upload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CompletableFuture<Response> uploadFilePresign(
            @NotNull UploadRequest uploadRequest,
            @PathParam("id") @NotNull Long id,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to upload file for user with ID: {}", id);
        authorize(id, securityContext);
        String userId = getUserId(securityContext);
        String bucketName = userId + bucket_suffix;

        // Generate presigned URL for uploading
        CompletableFuture<URL> presignedUrlFuture = minioService.generatePresignedUploadUrl(bucketName, uploadRequest.filename(), uploadRequest.mimetype());

        // Construct the response based on the presigned URL
        return presignedUrlFuture.thenApply(presignedUrl -> {
            if (presignedUrl != null) {
                LOGGER.info("Generated presigned URL for upload: {}", presignedUrl);
                // Construct the response with the presigned URL
                return Response.ok(new PresignResponse(presignedUrl.toString())).build();
            } else {
                LOGGER.error("Error generating presigned URL for upload");
                // Construct an error response
                return Response.serverError().entity("Error generating presigned URL for upload").build();
            }
        });
    }

    @GET
    @Path("/presign/download")
    @Authenticated
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CompletableFuture<Response> downloadFilePresign(
            @NotNull DownloadRequest downloadRequest,
            @PathParam("id") @NotNull Long id,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to download file '{}' for user with ID: {}", downloadRequest.objectKey(), id);
        return CompletableFuture.supplyAsync(() -> {
            try {
                authorize(id, securityContext);
            } catch (ForbiddenException e) {
                // Check if file was shared with this user
                String userId = getUserId(securityContext);
                if (!fileSharingService.isFileSharedWithUser(id, Long.valueOf(userId), downloadRequest.objectKey())) {
                    // File is not shared with this user
                    String errorMessage = String.format("User '%s' is not authorized to access this resource", id);
                    LOGGER.error(errorMessage);
                    throw new ForbiddenException(errorMessage);
                } else {
                    LOGGER.info("User '{}' is accessing file '{}' shared by user '{}'", userId, downloadRequest.objectKey(), id);
                }
            }
            String bucketName = id + bucket_suffix;
            return minioService.generatePresignedDownloadUrl(bucketName, downloadRequest.objectKey())
                    .thenApply(presignedUrl -> Response.ok(new PresignResponse(presignedUrl.toString())).build())
                    .join(); // Since we are already inside a supplyAsync, it's safe to use join
        });
    }

    @POST
    @Authenticated
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public CompletableFuture<Response> uploadFile(
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
        });
    }


    @GET
    @Authenticated
    @Path("/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public CompletableFuture<Response> downloadFile(
            @PathParam("id") @NotNull Long id,
            @QueryParam("objectKey") @NotNull String objectKey,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to download file '{}' for user with ID: {}", objectKey, id);

        return CompletableFuture.supplyAsync(() -> {
            try {
                authorize(id, securityContext);
            } catch (ForbiddenException e) {
                // Check if the file was shared with this user
                String userId = getUserId(securityContext);
                if (!fileSharingService.isFileSharedWithUser(id, Long.valueOf(userId), objectKey)) {
                    // File is not shared with this user
                    String errorMessage = String.format("User '%s' is not authorized to access this resource", id);
                    LOGGER.error(errorMessage);
                    throw new ForbiddenException(errorMessage);
                } else {
                    LOGGER.info("User '{}' is accessing file '{}' shared by user '{}'", userId, objectKey, id);
                }
            }

            String bucketName = id + bucket_suffix; // Assuming bucket_suffix is properly configured
            CompletableFuture<byte[]> objectBytes = minioService.downloadObject(bucketName, objectKey);

            return objectBytes.thenApply(bytes -> {
                if (bytes != null) {
                    LOGGER.info("File '{}' downloaded successfully for user with ID: {}", objectKey, id);
                    Response.ResponseBuilder response = Response.ok(bytes);
                    response.header("Content-Disposition", "attachment;filename=" + objectKey);
                    return response.build();
                } else {
                    LOGGER.error("File '{}' not found for user with ID: {}", objectKey, id);
                    return Response.status(Response.Status.NOT_FOUND).entity("File not found").build();
                }
            }).join();
        });
    }


    @POST
    @Authenticated
    @Path("/folder")
    @Consumes(MediaType.APPLICATION_JSON)
    public CompletableFuture<Response> createEmptyFolder(
            @PathParam("id") @NotNull Long id,
            @QueryParam("folderName") @NotNull String folderName,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to create empty folder '{}' for user with ID: {}", folderName, id);
        authorize(id, securityContext);
        String userId = getUserId(securityContext);
        String bucketName = userId + bucket_suffix;

        CompletableFuture<String> folderCreationFuture = minioService.createEmptyFolder(bucketName, folderName);
        return folderCreationFuture.thenApply(response -> {
            if (response.equals("Folder created successfully")) {
                LOGGER.info("Empty folder '{}' created successfully for user with ID: {}", folderName, id);
                return Response.ok().status(Response.Status.CREATED).build();
            } else {
                LOGGER.error("Error creating empty folder '{}' for user with ID: {}. Error: {}", folderName, id, response);
                return Response.serverError().entity(response).build();
            }
        });
    }

    @GET
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public CompletableFuture<List<FileObject>> listFiles(
            @PathParam("id") @NotNull Long id,
            @QueryParam("suffix") String suffix,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to list files for user with ID: {}", id);
        authorize(id, securityContext);
        String bucketName = id + bucket_suffix;
        return minioService.listObjects(bucketName, suffix);
    }
    @DELETE
    @Authenticated
    public CompletableFuture<Response> deleteFile(
            @PathParam("id") @NotNull Long userId,
            @QueryParam("objectKey") @NotNull String objectKey,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to delete file '{}' for user with ID: {}", objectKey, userId);
        authorize(userId, securityContext);
        String bucketName = userId + bucket_suffix;
        return minioService.deleteObject(bucketName, objectKey)
                .thenApply(response -> {
                    if (response.startsWith("Object deleted successfully")) {
                        LOGGER.info("File '{}' deleted successfully for user with ID: {}", objectKey, userId);
                        return Response.ok().build();
                    } else {
                        LOGGER.error("Error deleting file '{}' for user with ID: {}. Error: {}", objectKey, userId, response);
                        return Response.serverError().entity(response).build();
                    }
                });
    }
    @PUT
    @Authenticated
    public CompletableFuture<Response> renameFile(
            @PathParam("id") @NotNull Long userId,
            @QueryParam("objectKey") @NotNull String objectKey,
            @QueryParam("newName") @NotNull String newName,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to rename file '{}' for user with ID: {}", objectKey, userId);
        authorize(userId, securityContext);
        String bucketName = userId + bucket_suffix;
        return minioService.renameObject(bucketName, objectKey, newName)
                .thenApply(response -> {
                    if (response.startsWith("Object renamed successfully")) {
                        LOGGER.info("File '{}' renamed successfully for user with ID: {}", objectKey, userId);
                        return Response.ok().build();
                    } else {
                        LOGGER.error("Error renaming file '{}' for user with ID: {}. Error: {}", objectKey, userId, response);
                        return Response.serverError().entity(response).build();
                    }
                });
    }


    public record UploadRequest(String filename, String mimetype) {}

    public record DownloadRequest(String objectKey) {}

    public record PresignResponse(String presignedUrl) {}
    private void authorize(Long userId, SecurityContext securityContext) {
        AuthorizationUtils.checkAuthorization(userId, securityContext.getUserPrincipal().getName());
    }

    private String getUserId(SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }
}
