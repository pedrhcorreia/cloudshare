package isel.leic.unit;

import io.quarkus.test.junit.QuarkusTest;
import isel.leic.model.storage.FileObject;
import isel.leic.model.storage.FormData;
import isel.leic.service.MinioService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.services.s3.model.Bucket;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MinioServiceTest {

    @Inject
    MinioService minioService;

    @Test
    @Order(1)
    public void testCreateBucket() throws ExecutionException, InterruptedException {
        String bucketName = "test-bucket";
        CompletableFuture<String> createBucketFuture = minioService.createBucket(bucketName);
        String result = createBucketFuture.get();
        assertTrue(result.startsWith("Bucket created successfully"), "Bucket creation failed");
    }

    @Test
    @Order(2)
    public void testListBuckets() throws ExecutionException, InterruptedException {
        CompletableFuture<List<Bucket>> listBucketsFuture = minioService.listBuckets();
        List<Bucket> buckets = listBucketsFuture.get();
        assertTrue(buckets.size() > 0, "No buckets found");
    }

    @Test
    @Order(3)
    public void testUploadObject() throws ExecutionException, InterruptedException {
        // Create FormData
        FormData formData = new FormData();
        formData.data = new File("src/main/resources/test-file.txt");
        formData.filename = "test-file.txt";
        formData.mimetype = "text/plain";

        // Upload the test object and wait for completion
        CompletableFuture<String> uploadFuture = minioService.uploadObject("test-bucket", formData);
        String result = uploadFuture.get();
        assertTrue(result.startsWith("Object uploaded successfully"), "Object upload failed");
    }

    @Test
    @Order(4)
    public void testListObjectsAndDeleteObject() throws ExecutionException, InterruptedException {
        // List objects in the bucket
        CompletableFuture<List<FileObject>> listObjectsFuture = minioService.listObjects("test-bucket", null);
        List<FileObject> response = listObjectsFuture.get();

        // Find the object named "test-file.txt" and get its object key
        String objectKey = null;
        for (FileObject fileObject : response) {
            if (fileObject.getObjectKey().equals("test-file.txt")) {
                objectKey = fileObject.getObjectKey();
                break;
            }
        }

        // Ensure the object key is found
        assertNotNull(objectKey, "Object key not found");

        // Attempt to delete the object
        CompletableFuture<String> deleteObjectFuture = minioService.deleteObject("test-bucket", objectKey);
        String deleteResult = deleteObjectFuture.get();

        // Assert that the deletion was successful
        assertTrue(deleteResult.startsWith("Object deleted successfully"), "Object deletion failed");
        minioService.deleteBucket("test-bucket");
    }
}
