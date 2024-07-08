package isel.leic.service;


import isel.leic.model.storage.FileObject;
import isel.leic.model.storage.FormData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@ApplicationScoped
public class MinioService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioService.class);

    @Inject
    S3AsyncClient minioClient;

    @Inject
    S3Presigner presigner;

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

    public CompletableFuture<String> createEmptyFolder(String bucketName, String folderName) {
        LOGGER.info("Creating empty folder: {} in bucket: {}", folderName, bucketName);

        try {
            File emptyFile = File.createTempFile(folderName, ".tmp");
            try (FileOutputStream fos = new FileOutputStream(emptyFile)) {
                // Write zero bytes
            }
            emptyFile.deleteOnExit();

            FormData formData = new FormData();
            formData.data = emptyFile;
            formData.filename = folderName + "/";
            formData.mimetype = "application/x-directory";

            return uploadObject(bucketName, formData).thenApply(result -> {
                if (result.startsWith("Object uploaded successfully")) {
                    LOGGER.info("Empty folder created successfully: {} in bucket: {}", folderName, bucketName);
                    return "Folder created successfully";
                } else {
                    String errorMessage = "Failed to create folder: " + folderName + " in bucket: " + bucketName;
                    LOGGER.error(errorMessage);
                    throw new CompletionException(new Exception(errorMessage));
                }
            }).exceptionally(ex -> {
                LOGGER.error("Exception occurred while creating empty folder: {} in bucket: {}", folderName, bucketName, ex);
                return "Failed to create folder";
            });
        } catch (Exception e) {
            LOGGER.error("Exception occurred while preparing to create empty folder: {} in bucket: {}", folderName, bucketName, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<String> deleteBucket(String bucketName) {
        LOGGER.info("Deleting bucket: {}", bucketName);
        return listObjects(bucketName, null)
                .thenCompose(objects -> {
                    if (!objects.isEmpty()) {
                        return CompletableFuture.allOf(objects.stream()
                                        .map(object -> deleteObject(bucketName, object.getObjectKey())).toArray(CompletableFuture[]::new))
                                .thenApply(ignore -> {
                                    LOGGER.info("All objects deleted successfully from bucket: {}", bucketName);
                                    return "All objects deleted successfully";
                                });
                    } else {
                        LOGGER.info("No objects found in bucket: {}", bucketName);
                        return CompletableFuture.completedFuture("No objects found");
                    }
                })
                .thenCompose(ignore -> deleteBucketWithoutObjects(bucketName));
    }

    private CompletableFuture<String> deleteBucketWithoutObjects(String bucketName) {
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

    public CompletableFuture<URL> generatePresignedUploadUrl(String bucketName, String objectKey, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();

        return CompletableFuture.supplyAsync(() -> {
            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(r -> r
                    .signatureDuration(Duration.ofMinutes(15))
                    .putObjectRequest(objectRequest));

            return presignedRequest.url();
        });
    }

    public CompletableFuture<URL> generatePresignedDownloadUrl(String bucketName, String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        return CompletableFuture.supplyAsync(() -> {
            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(r -> r
                    .signatureDuration(Duration.ofMinutes(15))
                    .getObjectRequest(getObjectRequest));

            return presignedRequest.url();
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

    public CompletableFuture<Boolean> doesObjectExist(String bucketName, String objectKey) {
        LOGGER.info("Verifying if object '{}' exists in bucket: {}", objectKey, bucketName);
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        return minioClient.headObject(request)
                .thenApply(response -> {
                    LOGGER.info("Object '{}' exists in bucket: {}", objectKey, bucketName);
                    return true;
                })
                .exceptionally(throwable -> {
                    if (throwable instanceof NoSuchKeyException) {
                        LOGGER.info("Object '{}' does not exist in bucket: {}", objectKey, bucketName);
                        return false;
                    } else {
                        LOGGER.error("Error verifying if object '{}' exists in bucket '{}': {}", objectKey, bucketName, throwable.getMessage());
                        throw new RuntimeException("Error verifying if object exists", throwable);
                    }
                });
    }




}
