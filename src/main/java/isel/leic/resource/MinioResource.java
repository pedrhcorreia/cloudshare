package isel.leic.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.common.constraint.NotNull;
import isel.leic.model.storage.FileObject;
import isel.leic.model.storage.FormData;
import isel.leic.model.storage.MultipartChunkForm;
import isel.leic.service.FileSharingService;
import isel.leic.service.MinioService;
import isel.leic.utils.AuthorizationUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;

@Path("/user/{id}/object")
public class MinioResource {

    @Inject
    MinioService minioService;
    @Inject
    FileSharingService fileSharingService;
    @ConfigProperty(name = "user.bucket.suffix")
    String bucket_suffix;

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioResource.class);

    @GET
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public List<FileObject> listFiles(
            @PathParam("id") @NotNull Long id,
            @QueryParam("suffix") String suffix,
            @QueryParam("delimiter") String delimiter,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to list files for user with ID: {}", id);
        authorize(id, securityContext);
        String bucketName = id + bucket_suffix;
        return minioService.listObjects(bucketName, suffix,delimiter);
    }

    @DELETE
    @Authenticated
    public Response deleteFile(
            @PathParam("id") @NotNull Long userId,
            @QueryParam("objectKey") @NotNull String objectKey,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to delete file '{}' for user with ID: {}", objectKey, userId);
        try {
            authorize(userId, securityContext);
            String bucketName = userId + bucket_suffix;
            String response = minioService.deleteObject(bucketName, objectKey);

            if (response.startsWith("Object deleted successfully")) {
                LOGGER.info("File '{}' deleted successfully for user with ID: {}", objectKey, userId);
                return Response.ok().build();
            } else {
                LOGGER.error("Error deleting file '{}' for user with ID: {}. Error: {}", objectKey, userId, response);
                return Response.serverError().entity(response).build();
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred while processing delete file request for user with ID: {}", userId, e);
            return Response.serverError().entity("Error occurred while processing delete file request").build();
        }
    }

    @PUT
    @Authenticated
    public Response renameFile(
            @PathParam("id") @NotNull Long userId,
            @QueryParam("objectKey") @NotNull String objectKey,
            @QueryParam("newName") @NotNull String newName,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to rename file '{}' for user with ID: {}", objectKey, userId);
        try {
            authorize(userId, securityContext);
            String bucketName = userId + bucket_suffix;
            minioService.renameObject(userId, bucketName, objectKey, newName);
            LOGGER.info("File '{}' renamed successfully for user with ID: {}", objectKey, userId);
            return Response.ok().build();

        } catch (Exception e) {
            LOGGER.error("Error occurred while processing rename file request for user with ID: {}", userId, e);
            return Response.serverError().entity("Error occurred while processing rename file request").build();
        }
    }



    @POST
    @Authenticated
    @Path("/presign/upload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFilePresign(
            @NotNull UploadRequest uploadRequest,
            @PathParam("id") @NotNull Long id,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to upload file for user with ID: {}", id);
        try {
            authorize(id, securityContext);

            String userId = getUserId(securityContext);
            String bucketName = userId + bucket_suffix;

            URL presignedUrl = minioService.generatePreSignedUploadUrl(bucketName, uploadRequest.filename(), uploadRequest.mimetype());

            if (presignedUrl != null) {
                LOGGER.info("Generated presigned URL for upload: {}", presignedUrl);
                return Response.ok(new PresignResponse(presignedUrl.toString())).build();
            } else {
                LOGGER.error("Error generating presigned URL for upload");
                return Response.serverError().entity("Error generating presigned URL for upload").build();
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred while processing upload request for user with ID: {}", id, e);
            return Response.serverError().entity("Error occurred while processing upload request").build();
        }
    }



    @POST
    @Path("/presign/download")
    @Authenticated
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response downloadFilePresign(
            @NotNull DownloadRequest downloadRequest,
            @PathParam("id") @NotNull Long id,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to download file '{}' for user with ID: {}", downloadRequest.objectKey(), id);
        try {
            authorize(id, securityContext);

            String userId = getUserId(securityContext);
            String bucketName = id + bucket_suffix;

            if (fileSharingService.isFileSharedWithUser(id, Long.valueOf(userId), downloadRequest.objectKey())) {
                String errorMessage = String.format("User '%s' is not authorized to access this resource", id);
                LOGGER.error(errorMessage);
                return Response.status(Response.Status.FORBIDDEN).entity(errorMessage).build();
            } else {
                LOGGER.info("User '{}' is accessing file '{}' shared by user '{}'", userId, downloadRequest.objectKey(), id);
            }

            URL presignedUrl = minioService.generatePresignedDownloadUrl(bucketName, downloadRequest.objectKey());
            return Response.ok(new PresignResponse(presignedUrl.toString())).build();
        } catch (ForbiddenException e) {
            String errorMessage = String.format("User '%s' is not authorized to access this resource", id);
            LOGGER.error(errorMessage);
            return Response.status(Response.Status.FORBIDDEN).entity(errorMessage).build();
        } catch (Exception e) {
            LOGGER.error("Error occurred while processing download request for file '{}' for user with ID: {}", downloadRequest.objectKey(), id, e);
            return Response.serverError().entity("Error occurred while processing download request").build();
        }
    }

    @POST
    @Authenticated
    @Path("/folder")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createEmptyFolder(
            @PathParam("id") @NotNull Long id,
            @QueryParam("folderName") @NotNull String folderName,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to create empty folder '{}' for user with ID: {}", folderName, id);
        authorize(id, securityContext);
        String userId = getUserId(securityContext);
        String bucketName = userId + bucket_suffix;

        try {
            String response = minioService.createEmptyFolder(bucketName, folderName);
            if (response.equals("Folder created successfully")) {
                LOGGER.info("Empty folder '{}' created successfully for user with ID: {}", folderName, id);
                return Response.ok().status(Response.Status.CREATED).build();
            } else {
                LOGGER.error("Error creating empty folder '{}' for user with ID: {}. Error: {}", folderName, id, response);
                return Response.serverError().entity(response).build();
            }
        } catch (Exception e) {
            LOGGER.error("Exception occurred while creating empty folder '{}' for user with ID: {} and error message: {}", folderName, id, e.getMessage());
            return Response.serverError().entity("Failed to create folder").build();
        }
    }
    @POST
    @Authenticated
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(
            @NotNull FormData formData,
            @PathParam("id") @NotNull Long id,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to upload file for user with ID: {}", id);
        try {
            authorize(id, securityContext);
            String userId = getUserId(securityContext);
            String bucketName = userId + bucket_suffix;
            String response = minioService.uploadObject(bucketName, formData);
            if (response.startsWith("Object uploaded successfully")) {
                LOGGER.info("File uploaded successfully for user with ID: {}", id);
                return Response.ok().status(Response.Status.CREATED).build();
            } else {
                LOGGER.error("Error uploading file for user with ID: {}. Error: {}", id, response);
                return Response.serverError().entity(response).build();
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred while uploading file for user with ID: {}", id, e);
            return Response.serverError().entity("Error occurred while uploading file").build();
        }
    }


@GET
@Authenticated
@Path("/download")
@Produces(MediaType.APPLICATION_OCTET_STREAM)
public Response downloadFile(
        @PathParam("id") @NotNull Long id,
        @QueryParam("objectKey") @NotNull String objectKey,
        @Context SecurityContext securityContext
) {
    LOGGER.info("Received request to download file '{}' for user with ID: {}", objectKey, id);

    try {
        authorize(id, securityContext);
    } catch (ForbiddenException e) {
        String userId = getUserId(securityContext);
        if (fileSharingService.isFileSharedWithUser(id, Long.valueOf(userId), objectKey)) {
            String errorMessage = String.format("User '%s' is not authorized to access this resource", id);
            LOGGER.error(errorMessage);
            return Response.status(Response.Status.FORBIDDEN).entity(errorMessage).build();
        } else {
            LOGGER.info("User '{}' is accessing file '{}' shared by user '{}'", userId, objectKey, id);
        }
    }

    String bucketName = id + bucket_suffix;
    try {
        byte[] objectBytes = minioService.downloadObject(bucketName, objectKey);
        if (objectBytes != null && objectBytes.length > 0) {
            LOGGER.info("File '{}' downloaded successfully for user with ID: {}", objectKey, id);
            return Response.ok(objectBytes)
                    .header("Content-Length", String.valueOf(objectBytes.length))
                    .build();
        } else {
            LOGGER.error("File '{}' not found for user with ID: {}", objectKey, id);
            return Response.status(Response.Status.NOT_FOUND).entity("File not found").build();
        }
    } catch (Exception e) {
        LOGGER.error("Error occurred while downloading file '{}' for user with ID: {}", objectKey, id, e);
        return Response.serverError().entity("Error occurred while downloading file").build();
    }
}

    @GET
    @Authenticated
    @Path("/download/stream")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFileAsStream(
            @PathParam("id") @NotNull Long id,
            @QueryParam("objectKey") @NotNull String objectKey,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to download file '{}' for user with ID: {}", objectKey, id);

        try {
            authorize(id, securityContext);
        } catch (ForbiddenException e) {
            String userId = getUserId(securityContext);
            if (fileSharingService.isFileSharedWithUser(id, Long.valueOf(userId), objectKey)) {
                String errorMessage = String.format("User '%s' is not authorized to access this resource", id);
                LOGGER.error(errorMessage);
                return Response.status(Response.Status.FORBIDDEN).entity(errorMessage).build();
            } else {
                LOGGER.info("User '{}' is accessing file '{}' shared by user '{}'", userId, objectKey, id);
            }
        }

        String bucketName = id + bucket_suffix;
        try {
            StreamingOutput streamingOutput = minioService.downloadObjectAsStream(bucketName, objectKey);
            if (streamingOutput != null) {
                LOGGER.info("File '{}' downloaded successfully for user with ID: {}", objectKey, id);
                return Response.ok(streamingOutput)
                        .header("Content-Disposition", "attachment; filename=\"" + objectKey + "\"")
                        .build();
            } else {
                LOGGER.error("File '{}' not found for user with ID: {}", objectKey, id);
                return Response.status(Response.Status.NOT_FOUND).entity("File not found").build();
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred while downloading file '{}' for user with ID: {}", objectKey, id, e);
            return Response.serverError().entity("Error occurred while downloading file").build();
        }
    }

    @POST
    @Authenticated
    @Path("/multipart/start")
    @Produces(MediaType.APPLICATION_JSON)
    public Response startMultipartUpload(
            @PathParam("id") @NotNull Long id,
            @QueryParam("filename") @NotNull String filename,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to start multipart upload for user with ID: {}", id);
        try {
            authorize(id, securityContext);
            String userId = getUserId(securityContext);
            String bucketName = userId + bucket_suffix;

            String uploadId = minioService.startMultipartUpload(bucketName, filename);
            return Response.ok(new MultipartUploadResponse(uploadId)).build();
        } catch (Exception e) {
            LOGGER.error("Error occurred while starting multipart upload for user with ID: {}", id, e);
            return Response.serverError().entity("Error occurred while starting multipart upload").build();
        }
    }

    @POST
    @Authenticated
    @Path("/multipart/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadPart(
            @NotNull MultipartChunkForm chunkData,
            @PathParam("id") @NotNull Long id,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to upload part number {} for user with ID: {}", chunkData.getPartNumber(), id);
        try {
            authorize(id, securityContext);
            String userId = getUserId(securityContext);
            String bucketName = userId + bucket_suffix;

            String eTag = minioService.uploadPart(bucketName, chunkData.getData(), chunkData.getFilename(), chunkData.getUploadId(), chunkData.getPartNumber());
            LOGGER.info("Uploaded part number {} with ETag: {}", chunkData.getPartNumber(), eTag);

            return Response.ok(new PartUploadResponse(chunkData.getPartNumber(), eTag)).build();
        } catch (Exception e) {
            LOGGER.error("Error occurred while uploading part number {} for user with ID: {}", chunkData.getPartNumber(), id, e);
            return Response.serverError().entity("Error occurred while uploading part").build();
        }
    }

    @POST
    @Authenticated
    @Path("/multipart/complete")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response completeMultipartUpload(
            @PathParam("id") @NotNull Long id,
            @NotNull CompleteMultipartUploadRequest completeRequest,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to complete multipart upload for user with ID: {}", id);
        try {
            authorize(id, securityContext);
            String userId = getUserId(securityContext);
            String bucketName = userId + bucket_suffix;

            minioService.completeMultipartUpload(bucketName, completeRequest.filename(), completeRequest.uploadId());
            return Response.ok().build();
        } catch (Exception e) {
            LOGGER.error("Error occurred while completing multipart upload for user with ID: {}", id, e);
            return Response.serverError().entity("Error occurred while completing multipart upload").build();
        }
    }


    @POST
    @Authenticated
    @Path("/multipart/abort")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response abortMultipartUpload(
            @PathParam("id") @NotNull Long id,
            @NotNull CancelMultipartUploadRequest abortRequest,
            @Context SecurityContext securityContext
    ) {
        LOGGER.info("Received request to abort multipart upload for user with ID: {}", id);
        try {
            authorize(id, securityContext);
            String userId = getUserId(securityContext);
            String bucketName = userId + bucket_suffix;

            minioService.abortMultipartUpload(bucketName, abortRequest.filename(), abortRequest.uploadId());
            return Response.ok().build();
        } catch (Exception e) {
            LOGGER.error("Error occurred while aborting multipart upload for user with ID: {}", id, e);
            return Response.serverError().entity("Error occurred while aborting multipart upload").build();
        }
    }





    public record UploadRequest(String filename, String mimetype) {}

    public record DownloadRequest(String objectKey) {}

    public record PresignResponse(String presignedUrl) {}
    private void authorize(Long userId, SecurityContext securityContext) {
        AuthorizationUtils.checkAuthorization(userId, securityContext.getUserPrincipal().getName());
    }

    public record MultipartUploadResponse(String uploadId) {}

    public record PartUploadResponse(int partNumber, String eTag) {}

    public record CompleteMultipartUploadRequest(String uploadId, String filename) {}


    public record CancelMultipartUploadRequest(String uploadId, String filename) {}

    private String getUserId(SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
   }
}