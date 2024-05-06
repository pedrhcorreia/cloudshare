package isel.leic.resource;

import io.quarkus.security.Authenticated;
import isel.leic.model.objectstorage.FileObject;
import isel.leic.model.objectstorage.FormData;
import isel.leic.service.MinioService;
import isel.leic.utils.AuthorizationUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

@Path("/user/{id}/object")
public class MinioResource {

    @Inject
    MinioService minioService;
    @ConfigProperty(name = "user.bucket.suffix")
    String bucket_suffix;

    @POST
    @Authenticated
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(
            FormData formData,
            @PathParam("id") Long id,
            @Context SecurityContext securityContext
    ) {
        authorize(id, securityContext);
        String userId = getUserId(securityContext);
        String bucketName = userId + "_" + bucket_suffix; // Assuming bucket_suffix is properly configured
        String response = minioService.uploadObject(bucketName, formData);
        if (response.startsWith("Object uploaded successfully")) {
            return Response.ok().status(Response.Status.CREATED).build();
        } else {
            return Response.serverError().entity(response).build();
        }
    }

    @GET
    @Path("/{objectKey}")
    @Authenticated
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(
            @PathParam("id") Long id,
            @PathParam("objectKey") String objectKey,
            @Context SecurityContext securityContext
    ) {
        authorize(id, securityContext);
        String userId = getUserId(securityContext);
        String bucketName = userId + "_" + bucket_suffix; // Assuming bucket_suffix is properly configured
        byte[] objectBytes = minioService.downloadObject(bucketName, objectKey);
        if (objectBytes != null) {
            Response.ResponseBuilder response = Response.ok(objectBytes);
            response.header("Content-Disposition", "attachment;filename=" + objectKey);
            return response.build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("File not found").build();
        }
    }

    @GET
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public List<FileObject> listFiles(
            @PathParam("id") Long id,
            @Context SecurityContext securityContext
    ) {
        authorize(id, securityContext);
        String bucketName = id + "_" + bucket_suffix;
        List<FileObject> files = minioService.listObjects(bucketName, null);
        if (files != null) {
            return files;
        } else {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @DELETE
    @Path("/{objectKey}")
    @Authenticated
    public Response deleteFile(
            @PathParam("id") Long userId,
            @PathParam("objectKey") String objectKey,
            @Context SecurityContext securityContext
    ) {
        authorize(userId, securityContext);
        String bucketName = userId + "_" + bucket_suffix; // Assuming bucket_suffix is properly configured
        String response = minioService.deleteObject(bucketName, objectKey);
        if (response.startsWith("Object deleted successfully")) {
            return Response.ok().build();
        } else {
            return Response.serverError().entity(response).build();
        }
    }

    @PUT
    @Path("/{objectKey}")
    @Authenticated
    public Response renameFile(
            @PathParam("id") Long userId,
            @PathParam("objectKey") String objectKey,
            @QueryParam("newName") String newName,
            @Context SecurityContext securityContext
    ) {
        authorize(userId, securityContext);
        String bucketName = userId + "_" + bucket_suffix; // Assuming bucket_suffix is properly configured
        String response = minioService.renameObject(bucketName, objectKey, newName);
        if (response.startsWith("Object renamed successfully")) {
            return Response.ok().build();
        } else {
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
