package isel.leic;

import io.quarkus.test.junit.QuarkusTest;
import isel.leic.model.Group;
import isel.leic.model.User;
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
public class UserAndGroupServiceTest {

    @Inject
    UserService userService;
    @Inject
    GroupService groupService;


    @Test
    @Order(1)
    void testUserCreation(){
        User user1 = new User("user1", "password1");
        User user2 = new User("user2", "password2");
        userService.createUser(user1);
        userService.createUser(user2);

        // Call the UserService method being tested
        List<User> users = userService.findAll();

        // Check that the returned list is not null and contains the expected users
        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals("user1", users.get(0).getUsername());
        assertEquals("user2", users.get(1).getUsername());
    }

    @Test
    @Order(2)
    public void testCreateGroup(){
        User user = userService.findByUsername("user1");
        groupService.createGroup(new Group( "TestGroup",user.getId()));
        List<Group> groups = userService.findUserGroups(user.getId());
        assertEquals(1, groups.size(), "The list should contain exactly one group");

        // Assert that the group name matches
        assertEquals("TestGroup", groups.get(0).getName(), "The group name should match");
    }

    @Test
    @Order(3)
    public void testAddUserToGroup(){
        User user = userService.findByUsername("user1");
        User user2 = userService.findByUsername("user2");

        List<Group> groups = groupService.getGroupsOfUser(user.getId());
        assertEquals(1,groups.size());
        Group group = groups.get(0);
        groupService.addUserToGroup(user2.getId(), group.getId() );

        List<User> usersInGroup = groupService.getGroupMember(group.getId());
        assertEquals(1, usersInGroup.size());
        assertEquals(user2.getId(), usersInGroup.get(0).getId());
    }

    @Test
    @Order(4)
    public void testAddExistingUserToGroup() {
        User user = userService.findByUsername("user1");
        User user2 = userService.findByUsername("user2");
        Group group = userService.findUserGroups(user.getId()).get(0);
        // Try adding the user to the group again
        groupService.addUserToGroup(user2.getId(), group.getId());


        List<User> usersFromGroup = groupService.getGroupMember(group.getId());
        assertEquals(1, usersFromGroup.size()); // Ensure there are no duplicates
        assertEquals(user2.getId(), usersFromGroup.get(0).getId()); // Ensure the user is still in the group
    }

    @Test
    @Order(5)
    public void testAddMultipleUsersToGroup() {
        User user1 = userService.findByUsername("user1");
        User user2 = userService.findByUsername("user2");
        User user3 = new User("user3", "password3");
        userService.createUser(user3);
        Group group = userService.findUserGroups(user1.getId()).get(0);

        groupService.addUserToGroup(user3.getId(), group.getId());

        List<User> usersFromGroup = groupService.getGroupMember(group.getId());
        assertEquals(2, usersFromGroup.size());
        assertEquals(usersFromGroup.get(0).getId(), user2.getId());
        assertEquals(usersFromGroup.get(1).getId(), user3.getId());
    }

    @Test
    @Order(6)
    public void testRemoveUserFromGroup() {
        User user = userService.findByUsername("user1");
        User user2 = userService.findByUsername("user2");
        Group group = userService.findUserGroups(user.getId()).get(0);

        groupService.removeUserFromGroup(user2.getId(), group.getId());

        List<User> usersFromGroup = groupService.getGroupMember(group.getId());
        assertEquals(1, usersFromGroup.size());
        userService.delete(user2);
    }

    @Test
    @Order(7)
    public void testRemoveGroup() {
        User user = userService.findByUsername("user3");
        groupService.createGroup(new Group("TestGroup2",user.getId()));

        User user1 = userService.findByUsername("user1");
        Group group = userService.findUserGroups(user.getId()).get(0);
        groupService.addUserToGroup(user1.getId(),group.getId());

        groupService.removeGroup(group.getId());
        assertTrue(userService.findUserGroups(user.getId()).isEmpty());
        assertTrue(groupService.getGroupMember(group.getId()).isEmpty());

    }

    @Test
    @Order(8)
    public void testDeleteUser() {
        User user = userService.findByUsername("user1");
        User user3 = userService.findByUsername("user3");

        userService.delete(user3);

        // Verify that the user is deleted
        assertNull(userService.findById(user3.getId()));

        // Verify that the user is removed from all groups
        List<Group> groups = userService.findUserGroups(user.getId());
        List<User> usersFromGroup = groupService.getGroupMember(groups.get(0).getId());

        assertEquals(0, usersFromGroup.size());
        userService.delete(user);
    }



}
