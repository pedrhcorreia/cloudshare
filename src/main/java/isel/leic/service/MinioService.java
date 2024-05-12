package isel.leic.service;

import io.smallrye.mutiny.Multi;
import isel.leic.model.storage.FileObject;
import isel.leic.model.storage.FormData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import org.jboss.resteasy.reactive.RestMulti;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class MinioService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioService.class);

    @Inject
    S3AsyncClient minioClient;

    public CompletableFuture<List<Bucket>> listBuckets() {
        LOGGER.info("Listing buckets");
        ListBucketsRequest request = ListBucketsRequest.builder().build();
        return minioClient.listBuckets(request)
                .thenApply(ListBucketsResponse::buckets);
    }

    public CompletableFuture<String> createBucket(String bucketName) {
        LOGGER.info("Creating bucket: {}", bucketName);
        CreateBucketRequest request = CreateBucketRequest.builder()
                .bucket(bucketName)
                .build();
        return minioClient.createBucket(request)
                .thenApply(response -> {
                    LOGGER.info("Bucket created successfully: {}", bucketName);
                    return "Bucket created successfully: " + bucketName;
                });
    }

    public CompletableFuture<String> deleteBucket(String bucketName) {
        LOGGER.info("Deleting bucket: {}", bucketName);
        DeleteBucketRequest request = DeleteBucketRequest.builder()
                .bucket(bucketName)
                .build();
        return minioClient.deleteBucket(request)
                .thenApply(response -> {
                    LOGGER.info("Bucket deleted successfully: {}", bucketName);
                    return "Bucket deleted successfully: " + bucketName;
                });
    }

    public CompletableFuture<List<FileObject>> listObjects(String bucketName, String prefix) {
        LOGGER.info("Listing objects in bucket: {} {}", bucketName, prefix == null ? "" : "with prefix '" + prefix + "'");

        ListObjectsRequest request = ListObjectsRequest.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();
        return minioClient.listObjects(request)
                .thenApply(response -> response.contents().stream()
                        .map(FileObject::from)
                        .toList());
    }

    public CompletableFuture<String> uploadObject(String bucketName, FormData formData) {
        LOGGER.info("Uploading object '{}' to bucket: {}", formData.getFilename(), bucketName);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(formData.getFilename())
                .contentType(formData.getMimetype())
                .build();
        return minioClient.putObject(request, AsyncRequestBody.fromFile(formData.getData()))
                .thenApply(response -> {
                    LOGGER.info("Object uploaded successfully: {}", formData.getFilename());
                    return "Object uploaded successfully: " + formData.getFilename();
                });
    }

    public CompletableFuture<byte[]> downloadObject(String bucketName, String objectKey) {
        LOGGER.info("Downloading object '{}' from bucket: {}", objectKey, bucketName);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        return minioClient.getObject(request, AsyncResponseTransformer.toBytes())
                .thenApply(BytesWrapper::asByteArray);
    }


    public CompletableFuture<String> deleteObject(String bucketName, String objectKey) {
        LOGGER.info("Deleting object '{}' from bucket: {}", objectKey, bucketName);
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        return minioClient.deleteObject(request)
                .thenApply(response -> {
                    LOGGER.info("Object deleted successfully: {}", objectKey);
                    return "Object deleted successfully: " + objectKey;
                });
    }

    public CompletableFuture<String> renameObject(String bucketName, String objectKey, String newObjectKey) {
        LOGGER.info("Renaming object '{}' in bucket '{}' to '{}'", objectKey, bucketName, newObjectKey);

        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(objectKey)
                .destinationBucket(bucketName)
                .destinationKey(newObjectKey)
                .build();

        CompletableFuture<CopyObjectResponse> copyResponse = minioClient.copyObject(copyRequest);

        return copyResponse.thenCompose(response ->
                deleteObject(bucketName, objectKey).thenApply(deleteResponse -> {
                    LOGGER.info("Object renamed successfully: '{}' to '{}'", objectKey, newObjectKey);
                    return "Object renamed successfully";
                }));
    }

}
