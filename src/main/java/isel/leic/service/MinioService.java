package isel.leic.service;

import isel.leic.model.minio.FileObject;
import isel.leic.model.minio.FormData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

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

    public List<FileObject> listObjects(String bucketName) {
        LOGGER.info("Listing objects in bucket: {}", bucketName);
        try {
            ListObjectsRequest request = ListObjectsRequest.builder()
                    .bucket(bucketName)
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
}
