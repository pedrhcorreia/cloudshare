package isel.leic.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.common.constraint.NotNull;
import io.smallrye.mutiny.Uni;
import isel.leic.model.storage.FileObject;
import isel.leic.model.storage.FormData;
import isel.leic.service.FileSharingService;
import isel.leic.service.MinioService;
import isel.leic.utils.AnonymousAccessUtils;
import isel.leic.utils.AuthorizationUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.Map;
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
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public CompletableFuture<Response> uploadFile(
            @NotNull FormData formData,
            @PathParam("id") @NotNull Long id,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to upload file for user with ID: {}", id);
        authorize(id, securityContext);
        String userId = getUserId(securityContext);
        String bucketName = userId + bucket_suffix;

        // Generate presigned URL for uploading
        CompletableFuture<URL> presignedUrlFuture = minioService.generatePresignedUploadUrl(bucketName, formData.getFilename(), formData.getMimetype());

        // Construct the response based on the presigned URL
        return presignedUrlFuture.thenApply(presignedUrl -> {
            if (presignedUrl != null) {
                LOGGER.info("Generated presigned URL for upload: {}", presignedUrl);
                // Construct the response with the presigned URL
                return Response.ok(presignedUrl.toString()).build();
            } else {
                LOGGER.error("Error generating presigned URL for upload");
                // Construct an error response
                return Response.serverError().entity("Error generating presigned URL for upload").build();
            }
        });
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{objectKey}")
    public Uni<Response> downloadFile(
            @PathParam("id") @NotNull Long id,
            @PathParam("objectKey") @NotNull String objectKey,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to download file '{}' for user with ID: {}", objectKey, id);
        return Uni.createFrom().item(() -> {
                    try {
                        authorize(id, securityContext);
                    } catch (ForbiddenException e) {
                        // Check if file was shared with this user
                        String userId = getUserId(securityContext);
                        return fileSharingService.isFileSharedWithUser(id, Long.valueOf(userId), objectKey)
                                .map(shared -> {
                                    if (!shared) {
                                        // File is not shared with this user
                                        String errorMessage = String.format("User '%s' is not authorized to access this resource", id);
                                        LOGGER.error(errorMessage);
                                        throw new ForbiddenException(errorMessage);
                                    } else {
                                        LOGGER.info("User '{}' is accessing file '{}' shared by user '{}'", userId, objectKey, id);
                                        return true;
                                    }
                                });
                    }
                    return Uni.createFrom().item(true);
                })
                .onItem().transformToUni(ignored -> {
                    String bucketName = id + bucket_suffix;
                    return Uni.createFrom().completionStage(minioService.generatePresignedDownloadUrl(bucketName, objectKey))
                            .map(presignedUrl -> {
                                LOGGER.info("Presigned download URL generated successfully for file '{}' belonging to user with ID: {}", objectKey, id);
                                return Response.ok().entity(presignedUrl).build();
                            });
                });
    }
    @GET
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
    @Path("/{objectKey}")
    public CompletableFuture<Response> deleteFile(
            @PathParam("id") @NotNull Long userId,
            @PathParam("objectKey") @NotNull String objectKey,
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
    @Path("/{objectKey}")
    public CompletableFuture<Response> renameFile(
            @PathParam("id") @NotNull Long userId,
            @PathParam("objectKey") @NotNull String objectKey,
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
    @POST
    @Path("/{objectKey}/anonymous-link")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CompletableFuture<Response> getAnonymousLink(
            @PathParam("id") @NotNull Long userId,
            @PathParam("objectKey") @NotNull String objectKey,
            @Context SecurityContext securityContext,
            @RequestBody @NotNull Map<String, Long> requestBody
    ) {
        LOGGER.info("Received request to generate anonymous access link for file '{}' for user with ID: {}", objectKey, userId);
        authorize(userId, securityContext);
        Long expirationTime = System.currentTimeMillis() + requestBody.get("expiration");
        String token = AnonymousAccessUtils.encodeToken(expirationTime, userId, objectKey);

        String jsonResponse = String.format("{\"token\": \"%s\"}", token);

        return CompletableFuture.completedFuture(Response.ok().entity(jsonResponse).build());
    }


    private void authorize(Long userId, SecurityContext securityContext) {
        AuthorizationUtils.checkAuthorization(userId, securityContext.getUserPrincipal().getName());
    }

    private String getUserId(SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }
}
