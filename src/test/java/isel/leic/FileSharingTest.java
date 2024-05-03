package isel.leic;

import io.quarkus.test.junit.QuarkusTest;
import isel.leic.exceptions.DuplicateFileSharingException;
import isel.leic.model.FileSharing;
import isel.leic.model.User;
import isel.leic.service.FileSharingService;
import isel.leic.service.UserService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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

    private static Long fileShareID;

    @Test
    @Order(1)
    public void testCreateUsersAndShareFile() {
        // Create two users
        User user1 = new User("user1", "password1");
        User user2 = new User("user2", "password2");
        userService.persist(user1);
        userService.persist(user2);

        // Share a file between the users
        String filename = "example.txt";
        fileSharingService.shareFile(user1, user2, filename);

        // Retrieve files shared by user1 and user2
        List<FileSharing> filesSharedByUser1 = userService.getFilesSharedByUser("user1");
        List<FileSharing> filesSharedToUser2 = userService.getFilesSharedToUser("user2");

        // Assert that the file sharing entries were retrieved successfully
        assertNotNull(filesSharedByUser1);
        assertNotNull(filesSharedToUser2);
        assertEquals(1, filesSharedByUser1.size());
        assertEquals(1, filesSharedToUser2.size());
        assertEquals(filename, filesSharedByUser1.get(0).getFilename());
        assertEquals(filename, filesSharedToUser2.get(0).getFilename());
        fileShareID = filesSharedByUser1.get(0).getId();
    }

    @Test
    @Order(2)
    public void testCreateDuplicateFileSharingEntry() {
        // Retrieve users
        User user1 = userService.findByUsername("user1");
        User user2 = userService.findByUsername("user2");

        // Try to share the same file between the same users
        String filename = "example.txt";
        assertThrows(DuplicateFileSharingException.class, () -> {
            fileSharingService.shareFile(user1, user2, filename);
        });
    }
    @Test
    @Order(3)
    public void testDeleteFileSharingAndUsers() {

        // Delete file sharing entries
        fileSharingService.unshareFile(fileShareID);
        // Retrieve users
        User user1 = userService.findByUsername("user1");
        User user2 = userService.findByUsername("user2");
        // Check if file sharing entries are deleted
        List<FileSharing> filesSharedByUser1 = userService.getFilesSharedByUser("user1");
        List<FileSharing> filesSharedToUser2 = userService.getFilesSharedToUser("user2");
        assertEquals(0, filesSharedByUser1.size());
        assertEquals(0, filesSharedToUser2.size());
        // Delete users
        userService.delete(user1);
        userService.delete(user2);
    }

}
