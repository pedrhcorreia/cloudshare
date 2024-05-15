package isel.leic.resource;

import io.smallrye.common.constraint.NotNull;
import isel.leic.service.MinioService;
import isel.leic.service.UserService;
import isel.leic.utils.AnonymousAccessUtils;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Path("/anonymous")
public class AnonymousAccessResource {

    @Inject
    MinioService minioService;
    @Inject
    UserService userService;
    @ConfigProperty(name = "user.bucket.suffix")
    String bucket_suffix;
    private static final Logger LOGGER = LoggerFactory.getLogger(AnonymousAccessResource.class);

    @GET
    @Path("/info")
    @PermitAll
    public Response getFileInformation(@QueryParam("token") @NotNull String token) {
        LOGGER.info("Received request to get file information for token: {}", token);

        // Decode the token and verify integrity
        Map<String, Object> decodedToken = AnonymousAccessUtils.decodeToken(token);

        LOGGER.info("Token decoded successfully.");

        // Extract file information from the decoded token
        String fileName = (String) decodedToken.get("fileName");
        int userId =(int) decodedToken.get("sharedByUserId");

        LOGGER.info("Retrieving file information for file '{}' shared by user with ID: {}", fileName, userId);

        CompletableFuture<Boolean> fileExists = minioService.doesObjectExist(userId + bucket_suffix, fileName);
        if (!fileExists.join()) {
            LOGGER.warn("File '{}' not found in bucket '{}'.", fileName, userId + bucket_suffix);
            return Response.status(Response.Status.NOT_FOUND).entity("File not found").build();
        }

        LOGGER.info("File '{}' found in bucket '{}'.", fileName, userId + bucket_suffix);

        String username = userService.findById((long) userId).getUsername();

        LOGGER.info("Retrieved username '{}' for user with ID: {}", username, userId);

        Map<String, Object> fileInfo = Map.of(
                "fileName", fileName,
                "userId", userId,
                "username", username
        );

        LOGGER.info("File information retrieved successfully.");

        return Response.ok().entity(fileInfo).build();
    }

    @GET
    @Path("/download")
    @PermitAll
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(@QueryParam("token") @NotNull String token) {
        LOGGER.info("Received request to download file based on token: {}", token);

        // Decode the token and verify integrity
        Map<String, Object> decodedToken = AnonymousAccessUtils.decodeToken(token);

        LOGGER.info("Token decoded successfully.");

        // Extract file information from the decoded token
        String fileName = (String) decodedToken.get("fileName");
        int userId = (int) decodedToken.get("sharedByUserId");

        LOGGER.info("Retrieving file '{}' that belongs to user with ID: {}", fileName, userId);

        // Check if the file exists
        CompletableFuture<Boolean> fileExists = minioService.doesObjectExist(userId + bucket_suffix, fileName);
        if (!fileExists.join()) {
            LOGGER.warn("File '{}' not found in bucket '{}'.", fileName, userId + bucket_suffix);
            return Response.status(Response.Status.NOT_FOUND).entity("File not found").build();
        }

        LOGGER.info("File '{}' found in bucket '{}'.", fileName, userId + bucket_suffix);

        // Download the file
        String bucketName = userId + bucket_suffix;
        CompletableFuture<byte[]> fileBytes = minioService.downloadObject(bucketName, fileName);
        if (fileBytes.join() != null) {
            LOGGER.info("File '{}' belonging to user with ID: {} downloaded anonymously with success.", fileName, userId);
            Response.ResponseBuilder response = Response.ok(fileBytes.join());
            response.header("Content-Disposition", "attachment;filename=" + fileName);
            return response.build();
        } else {
            LOGGER.error("Error downloading file '{}' belonging to  user with ID: {} anonymously", fileName, userId);
            return Response.serverError().entity("Error downloading file").build();
        }
    }
}
