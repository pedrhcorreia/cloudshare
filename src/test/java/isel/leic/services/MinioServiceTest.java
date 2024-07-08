package isel.leic.services;

import io.quarkus.test.junit.QuarkusTest;
import isel.leic.model.storage.FileObject;
import isel.leic.model.storage.FormData;
import isel.leic.service.MinioService;
import jakarta.inject.Inject;

import org.junit.jupiter.api.*;
import software.amazon.awssdk.services.s3.model.Bucket;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

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
    public void testCreateFolder() {
        String bucketName = "test-bucket";
        String result = minioService.createEmptyFolder(bucketName, "home/");
        assertTrue(result.startsWith("Folder created successfully"), "Folder creation failed");
    }

    @Test
    @Order(3)
    public void testListBuckets() {
        List<Bucket> buckets = minioService.listBuckets();
        assertTrue(buckets.size() > 0, "No buckets found");
    }

    @Test
    @Order(4)
    public void testUploadObject() {
        FormData formData = new FormData();
        formData.data = new File("src/main/resources/test-file.txt");
        formData.filename = "home/test-file.txt";
        formData.mimetype = "text/plain";

        String result = minioService.uploadObject("test-bucket", formData);
        assertTrue(result.startsWith("Object uploaded successfully"), "Object upload failed");
    }

    @Test
    @Order(5)
    public void testDownloadFile() throws IOException {
        byte[] fileBytes = minioService.downloadObject("test-bucket", "home/test-file.txt");

        Path downloadedFilePath = Paths.get("src/main/resources/downloaded-test-file.txt");
        Files.write(downloadedFilePath, fileBytes);

        File downloadedFile = downloadedFilePath.toFile();
        assertTrue(downloadedFile.exists(), "Downloaded file does not exist");
        assertTrue(downloadedFile.length() > 0, "Downloaded file is empty");

        byte[] originalFileBytes = Files.readAllBytes(Paths.get("src/main/resources/test-file.txt"));
        assertArrayEquals(originalFileBytes, fileBytes, "Downloaded file content does not match original file content");

        Path renamedFilePath = Paths.get("src/main/resources/DOWNLOADED.txt");
        Files.write(renamedFilePath, fileBytes);

        Files.delete(renamedFilePath);
    }
    @Test
    @Order(6)
    public void testRenameFile() {
        String bucketName = "test-bucket";
        String oldObjectKey = "home/";
        String newObjectKey = "homeRenamed/";
        Long userId = 1L;


        minioService.renameObject(userId, bucketName, oldObjectKey, newObjectKey);

        // Verify the old object does not exist
        List<FileObject> objects = minioService.listObjects(bucketName, "home", null);
        boolean oldObjectExists = objects.stream().anyMatch(obj -> obj.getObjectKey().equals(oldObjectKey));
        assertFalse(oldObjectExists, "Old object still exists after renaming");

        // Verify the new object exists
        boolean newObjectExists = objects.stream().anyMatch(obj -> obj.getObjectKey().equals(newObjectKey));
        assertTrue(newObjectExists, "New object does not exist after renaming");


    }
    @Test
    @Order(7)
    public void testListObjectsAndDeleteObject() {
        List<FileObject> response = minioService.listObjects("test-bucket", "homeRenamed",null);

        String objectKey = null;
        for (FileObject fileObject : response) {
            if (fileObject.getObjectKey().equals("homeRenamed/test-file.txt")) {
                objectKey = fileObject.getObjectKey();
                assertNotNull(fileObject.getLastModified(), "Last modified is null");
                assertNotNull(fileObject.getETag(), "ETag is null");
                assertNotNull(fileObject.getStorageClass(), "Storage class is null");
                System.out.println(fileObject.getETag() + fileObject.getStorageClass() + fileObject.getLastModified());
                break;
            }
        }

        assertNotNull(objectKey, "Object key not found");

        String deleteResult = minioService.deleteObject("test-bucket", objectKey);
        assertTrue(deleteResult.startsWith("Object deleted successfully"), "Object deletion failed");

        minioService.deleteBucket("test-bucket");
    }
}
