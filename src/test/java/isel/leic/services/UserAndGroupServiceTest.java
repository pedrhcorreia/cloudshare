package isel.leic.services;

import io.quarkus.test.junit.QuarkusTest;
import isel.leic.exception.DuplicateResourceException;
import isel.leic.exception.GroupNotFoundException;
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


        List<User> users = userService.findAll();

        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals("user1", users.get(0).getUsername());
        assertEquals("user2", users.get(1).getUsername());
    }

    @Test
    @Order(2)
    public void testCreateGroup(){
        User user = userService.findByUsername("user1");
        groupService.createGroup(user.getId(), "TestGroup");
        List<Group> groups = userService.findUserGroups(user.getId());
        assertEquals(1, groups.size(), "The list should contain exactly one group");


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

        List<User> usersInGroup = groupService.getGroupMembers(group.getId());
        assertEquals(1, usersInGroup.size());
        assertEquals(user2.getId(), usersInGroup.get(0).getId());
    }

    @Test
    @Order(4)
    public void testAddExistingUserToGroup() {
        User user = userService.findByUsername("user1");
        User user2 = userService.findByUsername("user2");
        Group group = userService.findUserGroups(user.getId()).get(0);

        // Assert that adding an existing user to a group throws DuplicateResourceException
        assertThrows(DuplicateResourceException.class, () -> {
            groupService.addUserToGroup(user2.getId(), group.getId());
        });
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

        List<User> usersFromGroup = groupService.getGroupMembers(group.getId());
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

        List<User> usersFromGroup = groupService.getGroupMembers(group.getId());
        assertEquals(1, usersFromGroup.size());
        userService.removeUser(user2.getId());
    }
    @Test
    @Order(7)
    public void testUpdateGroupName() {
        User user = userService.findByUsername("user1");
        Group group = userService.findUserGroups(user.getId()).get(0);

        groupService.updateGroupName(group.getId(), "TestGroupUpdate");

        group = userService.findUserGroups(user.getId()).get(0);
        assertEquals("TestGroupUpdate", group.getName());

    }
    @Test
    @Order(8)
    public void testRemoveGroup() {
        User user = userService.findByUsername("user3");
        groupService.createGroup(user.getId(),"TestGroup2");

        User user1 = userService.findByUsername("user1");
        Group group = userService.findUserGroups(user.getId()).get(0);
        groupService.addUserToGroup(user1.getId(),group.getId());

        groupService.removeGroup(group.getId());
        assertEquals(0, userService.findUserGroups(user.getId()).size());
        assertThrows(GroupNotFoundException.class, () -> {
            groupService.getGroupMembers(group.getId());
        });

    }

    @Test
    @Order(9)
    public void testDeleteUser() {
        User user = userService.findByUsername("user1");
        User user3 = userService.findByUsername("user3");

        userService.removeUser(user3.getId());


        assertNull(userService.findById(user3.getId()));


        List<Group> groups = userService.findUserGroups(user.getId());
        assertEquals(0,groupService.getGroupMembers(groups.get(0).getId()).size());
        userService.removeUser(user.getId());
    }



}
