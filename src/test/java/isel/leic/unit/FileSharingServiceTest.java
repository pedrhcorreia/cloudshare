package isel.leic.unit;

import io.quarkus.test.junit.QuarkusTest;
import isel.leic.exceptions.DuplicateResourceException;
import isel.leic.exceptions.FileSharingNotFoundException;
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
public class FileSharingServiceTest {
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
    private static Long user3Id;

    private static Long groupId;

    @Test
    @Order(1)
    public void testCreateUsersAndShareFile() {

        User user1 = new User("user1", "password1");
        User user2 = new User("user2", "password2");
        userService.createUser(user1);
        userService.createUser(user2);
        user1Id = user1.getId();
        user2Id = user2.getId();

        String filename = "example.txt";
        fileSharingService.shareFileToUser(user1Id, user2Id, filename);

        List<FileSharing> filesSharedByUser1 = fileSharingService.getFilesSharedByUser(user1Id);
        List<FileSharing> filesSharedToUser2 = fileSharingService.getFilesSharedToUser(user2Id);

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

        String filename = "example.txt";
        assertThrows(DuplicateResourceException.class, () -> {
            fileSharingService.shareFileToUser(user1Id, user2Id, filename);
        });
    }

    @Test
    @Order(3)
    public void testShareFileToGroup(){
        groupService.createGroup(user1Id ,"Group1" );
        Group group = groupService.findByCreatorIdAndName(user1Id, "Group1").get();
        groupId = group.getId();

        User user3 = new User("user3", "password4");
        userService.createUser(user3);
        groupService.addUserToGroup(user3.getId(), groupId);


        String filename = "example.txt";
        fileSharingService.shareFileToGroup(user1Id,groupId, filename);
        groupId = group.getId();


        List<FileSharing> filesSharedToGroup = fileSharingService.getFilesSharedToUser(user3.getId());
        assertEquals(1, filesSharedToGroup.size());
        assertEquals(filename, filesSharedToGroup.get(0).getFilename());
        fileShareIdToGroup = filesSharedToGroup.get(0).getId();
        user3Id = user3.getId();
    }

    @Test
    @Order(4)
    public void testGetFilesSharedByUser(){
        List<FileSharing> filesSharedByUser1 = fileSharingService.getFilesSharedByUser(user1Id);
        assertNotNull(filesSharedByUser1);
        assertEquals(2, filesSharedByUser1.size());


    }
    @Test
    @Order(5)
    public void testDeleteFileSharingToUsers() {

        fileSharingService.unshareFile(fileShareIdToUser);


        assertEquals(fileSharingService.getFilesSharedByUser(user1Id).size(),1);

        assertThrows(FileSharingNotFoundException.class, () -> {
            fileSharingService.getFilesSharedToUser(user2Id);
        });
        userService.removeUser(user1Id);
        userService.removeUser(user2Id);
        userService.removeUser(user3Id);
    }


}
