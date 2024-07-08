package isel.leic.services;

import io.quarkus.test.junit.QuarkusTest;
import isel.leic.exception.DuplicateResourceException;
import isel.leic.model.Group;
import isel.leic.model.User;
import isel.leic.model.storage.FileObject;
import isel.leic.model.storage.FormData;
import isel.leic.model.FileSharingResponse;
import isel.leic.service.FileSharingService;
import isel.leic.service.GroupService;
import isel.leic.service.MinioService;
import isel.leic.service.UserService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileSharingServiceTest {
    @Inject
    FileSharingService fileSharingService;
    @Inject
    UserService userService;
    @Inject
    GroupService groupService;
    @Inject
    MinioService minioService;

    private static Long fileShareIdToUser;
    private static Long user1Id;
    private static Long user2Id;
    private static Long user3Id;
    private static Long groupId;

    @Test
    @Order(1)
    public void testCreateUsersAndShareFile() {
        // Create users
        User user1 = new User("user1", "password1");
        User user2 = new User("user2", "password2");
        userService.createUser(user1);
        userService.createUser(user2);
        user1Id = user1.getId();
        user2Id = user2.getId();

        // Upload file to S3
        String bucketName = user1Id + "-bucket";
        minioService.createBucket(bucketName);
        FormData formData = new FormData();
        formData.data = new File("src/main/resources/test-file.txt");
        formData.filename = "test-file.txt";
        formData.mimetype = "text/plain";

        String uploadResult = minioService.uploadObject(bucketName, formData);
        assertTrue(uploadResult.startsWith("Object uploaded successfully"), "Object upload failed");

        // Share file to user
        fileSharingService.shareFileToUser(user1Id, user2Id, formData.filename);
        String filename = formData.filename;

        // Retrieve shared files
        List<FileSharingResponse> filesSharedByUser1 = fileSharingService.getFilesSharedByUser(user1Id);
        List<FileSharingResponse> filesSharedToUser2 = fileSharingService.getFilesSharedToUser(user2Id);

        // Assertions
        assertNotNull(filesSharedByUser1);
        assertNotNull(filesSharedToUser2);
        assertEquals(1, filesSharedByUser1.size());
        assertEquals(1, filesSharedToUser2.size());

        FileSharingResponse responseByUser1 = filesSharedByUser1.get(0);
        FileSharingResponse responseToUser2 = filesSharedToUser2.get(0);

        FileObject fileObjectByUser1 = responseByUser1.getFileObject();
        FileObject fileObjectToUser2 = responseToUser2.getFileObject();

        assertEquals(filename, fileObjectByUser1.getObjectKey());
        assertEquals(filename, fileObjectToUser2.getObjectKey());

        // Assert the additional properties are not null
        assertNotNull(fileObjectByUser1.getLastModified());
        assertNotNull(fileObjectByUser1.getETag());
        assertNotNull(fileObjectByUser1.getStorageClass());

        fileShareIdToUser = responseByUser1.getFileSharing().getId();
    }

    @Test
    @Order(2)
    public void testCreateDuplicateFileSharingEntry() {
        String filename = "test-file.txt";
        assertThrows(DuplicateResourceException.class, () -> {
            fileSharingService.shareFileToUser(user1Id, user2Id, filename);
        });
    }


    @Test
    @Order(3)
    public void testShareFileToGroup() {
        groupService.createGroup(user1Id, "Group1");
        Group group = groupService.findByCreatorIdAndName(user1Id, "Group1").get();
        groupId = group.getId();

        User user3 = new User("user3", "password3");
        userService.createUser(user3);
        groupService.addUserToGroup(user3.getId(), groupId);

        String filename = "test-file.txt";
        fileSharingService.shareFileToGroup(user1Id, groupId, filename);
        groupId = group.getId();

        List<FileSharingResponse> filesSharedToGroup = fileSharingService.getFilesSharedToUser(user3.getId());
        assertEquals(1, filesSharedToGroup.size());
        assertEquals(filename, filesSharedToGroup.get(0).getFileObject().getObjectKey());

        user3Id = user3.getId();
    }
    @Test
    @Order(4)
    public void testUpdateFileSharingEntries() {
        String filename = "test-file.txt";
        minioService.renameObject(user1Id,user1Id+"-bucket", filename, "test-file-new.txt");

    }
    @Test
    @Order(5)
    public void testGetFilesSharedByUser() {
        List<FileSharingResponse> filesSharedByUser1 = fileSharingService.getFilesSharedByUser(user1Id);
        assertNotNull(filesSharedByUser1);
        assertEquals(2, filesSharedByUser1.size());

    }

    @Test
    @Order(6)
    public void testDeleteFileSharingToUsers() {
        fileSharingService.unshareFile(fileShareIdToUser);

        assertEquals(1, fileSharingService.getFilesSharedByUser(user1Id).size());
        assertEquals(0, fileSharingService.getFilesSharedToUser(user2Id).size());

        userService.removeUser(user1Id);
        userService.removeUser(user2Id);
        userService.removeUser(user3Id);
    }
}
