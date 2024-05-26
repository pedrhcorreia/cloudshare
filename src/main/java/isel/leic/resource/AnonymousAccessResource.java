package isel.leic.resource;

import io.smallrye.common.constraint.NotNull;
import io.smallrye.mutiny.Uni;
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
    public Uni<Response> getFileInformation(@QueryParam("token") @NotNull String token) {
        LOGGER.info("Received request to get file information for token: {}", token);

        // Decode the token and verify integrity
        Map<String, Object> decodedToken = AnonymousAccessUtils.decodeToken(token);

        LOGGER.info("Token decoded successfully.");

        // Extract file information from the decoded token
        String fileName = (String) decodedToken.get("fileName");
        Long userId = ((Number) decodedToken.get("sharedByUserId")).longValue();

        LOGGER.info("Retrieving file information for file '{}' shared by user with ID: {}", fileName, userId);

        return Uni.createFrom().completionStage(minioService.doesObjectExist(userId + bucket_suffix, fileName))
                .flatMap(fileExists -> {
                    if (!fileExists) {
                        LOGGER.warn("File '{}' not found in bucket '{}'.", fileName, userId + bucket_suffix);
                        return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).entity("File not found").build());
                    }

                    LOGGER.info("File '{}' found in bucket '{}'.", fileName, userId + bucket_suffix);

                    return userService.findById(userId)
                            .map(user -> {
                                String username = user.getUsername();

                                LOGGER.info("Retrieved username '{}' for user with ID: {}", username, userId);

                                Map<String, Object> fileInfo = Map.of(
                                        "fileName", fileName,
                                        "userId", userId,
                                        "username", username
                                );

                                LOGGER.info("File information retrieved successfully.");

                                return Response.ok().entity(fileInfo).build();
                            });
                });
    }

    @GET
    @Path("/download")
    @PermitAll
    public Uni<Response> downloadFile(@QueryParam("token") @NotNull String token) {
        LOGGER.info("Received request to generate presigned download URL based on token: {}", token);

        // Decode the token and verify integrity
        Map<String, Object> decodedToken = AnonymousAccessUtils.decodeToken(token);

        LOGGER.info("Token decoded successfully.");

        // Extract file information from the decoded token
        String fileName = (String) decodedToken.get("fileName");
        Long userId = ((Number) decodedToken.get("sharedByUserId")).longValue();

        LOGGER.info("Retrieving presigned download URL for file '{}' that belongs to user with ID: {}", fileName, userId);

        // Check if the file exists
        return Uni.createFrom().completionStage(minioService.doesObjectExist(userId + bucket_suffix, fileName))
                .flatMap(fileExists -> {
                    if (!fileExists) {
                        LOGGER.warn("File '{}' not found in bucket '{}'.", fileName, userId + bucket_suffix);
                        return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).entity("File not found").build());
                    }

                    LOGGER.info("File '{}' found in bucket '{}'.", fileName, userId + bucket_suffix);

                    String bucketName = userId + bucket_suffix;
                    return Uni.createFrom().completionStage(minioService.generatePresignedDownloadUrl(bucketName, fileName))
                            .map(presignedUrl -> {
                                LOGGER.info("Presigned download URL generated successfully for file '{}' belonging to user with ID: {}", fileName, userId);
                                return Response.ok().entity(presignedUrl).build();
                            });
                });
    }
}
