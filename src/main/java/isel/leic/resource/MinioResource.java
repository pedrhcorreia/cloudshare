package isel.leic.resource;


import isel.leic.service.MinioService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import software.amazon.awssdk.services.s3.model.Bucket;

import java.util.List;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MinioResource {
    @Inject
    MinioService minioService;

    @GET
    @Path("/buckets")
    public Response listBuckets() {
        List<Bucket> buckets = minioService.listBuckets();
        return Response.ok(buckets).build();
    }

    /*@POST
    @Path("/buckets")
    public Response createBucket(String bucketName) {
        String result = minioService.createBucket(bucketName);
        return Response.ok(result).build();
    }

    @DELETE
    @Path("/buckets/{bucketName}")
    public Response deleteBucket(@PathParam("bucketName") String bucketName) {
        String result = minioService.deleteBucket(bucketName);
        return Response.ok(result).build();
    }

    @GET
    @Path("/buckets/{bucketName}/objects")
    public Response listObjects(@PathParam("bucketName") String bucketName) {
        List<FileObject> objects = minioService.listObjects(bucketName);
        return Response.ok(objects).build();
    }

    @POST
    @Path("/buckets/{bucketName}/objects")
    public Response uploadObject(@PathParam("bucketName") String bucketName, FormData formData) {
        String result = minioService.uploadObject(bucketName, formData);
        return Response.ok(result).build();
    }

    @DELETE
    @Path("/buckets/{bucketName}/objects/{objectKey}")
    public Response deleteObject(@PathParam("bucketName") String bucketName, @PathParam("objectKey") String objectKey) {
        String result = minioService.deleteObject(bucketName, objectKey);
        return Response.ok(result).build();
    }*/


}
