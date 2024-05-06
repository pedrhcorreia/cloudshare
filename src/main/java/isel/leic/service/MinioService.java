package isel.leic.service;

import isel.leic.model.objectstorage.FileObject;
import isel.leic.model.objectstorage.FormData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class MinioService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioService.class);

    @Inject
    S3Client minioClient;

    public List<Bucket> listBuckets() {
        LOGGER.info("Listing buckets");
        ListBucketsResponse response = minioClient.listBuckets();
        return response.buckets();
    }

    public String createBucket(String bucketName) {
        LOGGER.info("Creating bucket: {}", bucketName);
        try {
            CreateBucketRequest request = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            minioClient.createBucket(request);
            LOGGER.info("Bucket created successfully: {}", bucketName);
            return "Bucket created successfully: " + bucketName;
        } catch (S3Exception e) {
            LOGGER.error("Error creating bucket {}: {}", bucketName, e.awsErrorDetails().errorMessage());
            return "Error creating bucket: " + e.awsErrorDetails().errorMessage();
        }
    }

    public String deleteBucket(String bucketName) {
        LOGGER.info("Deleting bucket: {}", bucketName);
        try {
            //TODO maybe delete every object inside bucket here
            DeleteBucketRequest request = DeleteBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            minioClient.deleteBucket(request);
            LOGGER.info("Bucket deleted successfully: {}", bucketName);
            return "Bucket deleted successfully: " + bucketName;
        } catch (S3Exception e) {
            LOGGER.error("Error deleting bucket {}: {}", bucketName, e.awsErrorDetails().errorMessage());
            return "Error deleting bucket: " + e.awsErrorDetails().errorMessage();
        }
    }

    public List<FileObject> listObjects(String bucketName, String prefix) {
        LOGGER.info("Listing objects in bucket: {} {}", bucketName, prefix == null? "" : "with prefix '" + prefix+"'");

        try {
            ListObjectsRequest request = ListObjectsRequest.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();
            ListObjectsResponse response = minioClient.listObjects(request);
            return response.contents().stream()
                    .map(FileObject::from)
                    .toList();
        } catch (S3Exception e) {
            LOGGER.error("Error listing objects in bucket {}: {}", bucketName, e.awsErrorDetails().errorMessage());
            return null; // Handle error properly
        }
    }

    public String uploadObject(String bucketName, FormData formData) {
        LOGGER.info("Uploading object '{}' to bucket: {}", formData.getFilename(), bucketName);
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(formData.getFilename())
                    .contentType(formData.getMimetype())
                    .build();
            minioClient.putObject(request, formData.getData().toPath());
            LOGGER.info("Object uploaded successfully: {}", formData.getFilename());
            return "Object uploaded successfully: " + formData.getFilename();
        } catch (S3Exception e) {
            LOGGER.error("Error uploading object {}: {}", formData.getFilename(), e.awsErrorDetails().errorMessage());
            return "Error uploading object: " + e.awsErrorDetails().errorMessage();
        }
    }

    public byte[] downloadObject(String bucketName, String objectKey) {
        LOGGER.info("Downloading object '{}' from bucket: {}", objectKey, bucketName);
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            ResponseInputStream<GetObjectResponse> responseInputStream = minioClient.getObject(getObjectRequest);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = responseInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            LOGGER.info("Object downloaded successfully: {}", objectKey);
            return outputStream.toByteArray();
        } catch (S3Exception | IOException e) {
            LOGGER.error("Error downloading object {}: {}", objectKey, e.getMessage());
            return null;
        }
    }

    public String deleteObject(String bucketName, String objectKey) {
        LOGGER.info("Deleting object '{}' from bucket: {}", objectKey, bucketName);
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            minioClient.deleteObject(deleteObjectRequest);
            LOGGER.info("Object deleted successfully: {}", objectKey);
            return "Object deleted successfully: " + objectKey;
        } catch (S3Exception e) {
            LOGGER.error("Error deleting object {}: {}", objectKey, e.awsErrorDetails().errorMessage());
            return "Error deleting object: " + e.awsErrorDetails().errorMessage();
        }
    }
    public String renameObject(String bucketName, String objectKey, String newObjectKey) {
        LOGGER.info("Renaming object '{}' in bucket '{}' to '{}'", objectKey, bucketName, newObjectKey);

        try {
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(objectKey)
                    .destinationBucket(bucketName)
                    .destinationKey(newObjectKey)
                    .build();

            CopyObjectResponse copyResponse = minioClient.copyObject(copyRequest);
            LOGGER.info("Object renamed successfully: '{}' to '{}'", objectKey, newObjectKey);

            // Delete the old object
            deleteObject(bucketName, objectKey);

            return copyResponse.copyObjectResult().toString();
        } catch (S3Exception e) {
            LOGGER.error("Error renaming object '{}': {}", objectKey, e.awsErrorDetails().errorMessage());
            return "Error renaming object: " + e.awsErrorDetails().errorMessage();
        }
    }

}
