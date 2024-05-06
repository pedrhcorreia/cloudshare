package isel.leic.unit;


import io.quarkus.test.junit.QuarkusTest;
import isel.leic.model.objectstorage.FileObject;
import isel.leic.model.objectstorage.FormData;
import isel.leic.service.MinioService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.services.s3.model.Bucket;
import java.io.File;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MinioServiceTest {

    @Inject
    MinioService minioService;

    @Test
    @Order(1)
    public void testCreateBucket() {
        String bucketName = "test-bucket";
        String result = minioService.createBucket(bucketName);
        assertTrue(result.startsWith("Bucket created successfully"), "Bucket creation failed");
    }

    @Test
    @Order(2)
    public void testListBuckets() {
        List<Bucket> buckets = minioService.listBuckets();
        assertTrue(buckets.size() > 0, "No buckets found");
    }

    @Test
    @Order(5)
    public void testDeleteBucket() {
        String bucketName = "test-bucket";
        String result = minioService.deleteBucket(bucketName);
        assertTrue(result.startsWith("Bucket deleted successfully"), "Bucket deletion failed");
    }

    @Test
    @Order(3)
    public void testUploadObject() {

        // Create FormData
        FormData formData = new FormData();
        formData.data = new File("src/main/resources/test-file.txt");
        formData.filename = "test-file.txt";
        formData.mimetype = "text/plain";

        // Upload the test object
        String result = minioService.uploadObject("test-bucket", formData);

        // Check if the upload was successful
        assertTrue(result.startsWith("Object uploaded successfully"), "Object upload failed");


    }
    @Test
    @Order(4)
    public void testListObjectsAndDeleteObject() {
        // List objects in the bucket
        List<FileObject> response = minioService.listObjects("test-bucket", null);

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
        String deleteResult = minioService.deleteObject("test-bucket", objectKey);

        // Assert that the deletion was successful
        assertTrue(deleteResult.startsWith("Object deleted successfully"), "Object deletion failed");
    }
}
