package isel.leic;

import io.quarkus.test.junit.QuarkusTest;
import isel.leic.exceptions.DuplicateFileSharingException;
import isel.leic.model.FileSharing;
import isel.leic.model.Group;
import isel.leic.model.User;
import isel.leic.service.FileSharingService;
import isel.leic.service.GroupService;
import isel.leic.service.UserService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileSharingTest {
    @Inject
    FileSharingService fileSharingService;
    @Inject
    UserService userService;
    @Inject
    GroupService groupService;

    private static Long fileShareIdToUser;
    private static Long fileShareIdToGroup;
    private static Long user1Id;
    private static Long user2Id;
    private static Long groupId;

    @Test
    @Order(1)
    public void testCreateUsersAndShareFile() {
        // Create two users
        User user1 = new User("user1", "password1");
        User user2 = new User("user2", "password2");
        userService.createUser(user1);
        userService.createUser(user2);
        user1Id = user1.getId();
        user2Id = user2.getId();
        // Share a file between the users
        String filename = "example.txt";
        fileSharingService.shareFileToUser(user1Id, user2Id, filename);
        // Access the filesSharedByUser and filesSharedToUser collections within the transactional context
        List<FileSharing> filesSharedByUser1 = userService.getFilesSharedByUser(user1Id);
        List<FileSharing> filesSharedToUser2 = userService.getFilesSharedToUser(user2Id);
        // Assert that the file sharing entries were retrieved successfully
        assertNotNull(filesSharedByUser1);
        assertNotNull(filesSharedToUser2);
        assertEquals(1, filesSharedByUser1.size());
        assertEquals(1, filesSharedToUser2.size());
        assertEquals(filename, filesSharedByUser1.get(0).getFilename());
        assertEquals(filename, filesSharedToUser2.get(0).getFilename());
        fileShareIdToUser = filesSharedByUser1.get(0).getId();
    }


    @Test
    @Order(2)
    public void testCreateDuplicateFileSharingEntry() {
        // Try to share the same file between the same users
        String filename = "example.txt";
        assertThrows(DuplicateFileSharingException.class, () -> {
            fileSharingService.shareFileToUser(user1Id, user2Id, filename);
        });
    }

    @Test
    @Order(3)
    public void testShareFileToGroup(){
        // Create a group
        Group group = new Group("Group1", user1Id);
        groupService.createGroup(group);
        groupId = group.getId();

        // Add user2 to the group
        groupService.addUserToGroup(user2Id, groupId);

        // Share a file with the group
        String filename = "example.txt";
        fileSharingService.shareFileToGroup(user1Id,groupId, filename);
        groupId = group.getId();
        // Check if the file sharing entry is created
        List<FileSharing> filesSharedToGroup = groupService.getFilesSharedToGroup(groupId);
        assertEquals(1, filesSharedToGroup.size());
        assertEquals(filename, filesSharedToGroup.get(0).getFilename());
        fileShareIdToGroup = filesSharedToGroup.get(0).getId();
    }

    @Test
    @Order(4)
    public void testGetFilesSharedByUser(){
        List<FileSharing> filesSharedByUser1 = userService.getFilesSharedByUser(user1Id);
        assertNotNull(filesSharedByUser1);
        assertEquals(2, filesSharedByUser1.size());


    }
    @Test
    @Order(5)
    public void testDeleteFileSharingToUsers() {
        // Delete file sharing entries
        fileSharingService.unshareFile(fileShareIdToUser);
        // Check if file sharing entries are deleted
        List<FileSharing> filesSharedByUser1 = userService.getFilesSharedByUser(user1Id);
        List<FileSharing> filesSharedToUser2 = userService.getFilesSharedToUser(user2Id);
        assertEquals(1, filesSharedByUser1.size());
        assertEquals(0, filesSharedToUser2.size());
    }



    @Test
    @Order(6)
    public void testDeleteFileSharingToGroup(){
        fileSharingService.unshareFile(fileShareIdToGroup);
        // Check if file sharing entries are deleted
        List<FileSharing> filesSharedByUser1 = userService.getFilesSharedByUser(user1Id);
        List<FileSharing> filesSharedToGroup = groupService.getFilesSharedToGroup(groupId);
        assertEquals(0, filesSharedByUser1.size());
        assertEquals(0, filesSharedToGroup.size());
        userService.delete(userService.findById(user1Id));
        userService.delete(userService.findById(user2Id));
    }
}
