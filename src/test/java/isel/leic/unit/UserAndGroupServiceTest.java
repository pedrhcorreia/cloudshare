package isel.leic.unit;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.vertx.SafeVertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import isel.leic.exception.DuplicateResourceException;
import isel.leic.exception.GroupNotFoundException;
import isel.leic.exception.MembersNotFoundException;
import isel.leic.model.Group;
import isel.leic.model.User;
import isel.leic.service.GroupService;
import isel.leic.service.UserService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserAndGroupServiceTest {

    @Inject
    UserService userService;
    @Inject
    GroupService groupService;
    @Inject
    Vertx vertx;

    /*@Test
    @Order(1)
    @SafeVertxContext
    void testUserCreation() {
        vertx.executeBlocking(promise -> {
            User user1 = new User("user1", "password1");
            User user2 = new User("user2", "password2");

            Uni<User> createUser1Uni = userService.createUser(user1);
            Uni<User> createUser2Uni = userService.createUser(user2);

            Uni<Tuple2<User, User>> usersUni = Uni.combine().all().unis(createUser1Uni, createUser2Uni).asTuple();

            usersUni
                    .onItem().transformToUni(usersTuple -> {
                        List<User> users = Arrays.asList(usersTuple.getItem1(), usersTuple.getItem2());
                        assertNotNull(users);
                        assertEquals(2, users.size());
                        assertEquals("user1", users.get(0).getUsername());
                        assertEquals("user2", users.get(1).getUsername());
                        return Uni.createFrom().item(users);
                    })
                    .subscribe().with(
                            promise::complete,
                            promise::fail
                    );
        }, res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }
        });
    }

*/
    @Test
    @Order(2)
    public void testCreateGroup() {
        Uni<User> userUni = userService.findByUsername("user1")
                .onItem().ifNull().failWith(new IllegalStateException("User 'user1' not found"));

        Uni<Group> groupUni = userUni
                .onItem().transformToUni(user -> groupService.createGroup(user.getId(), "TestGroup"));

        Uni<List<Group>> groupsUni = groupUni
                .onItem().transformToUni(group -> userService.findUserGroups(group.getCreatorId()));

        List<Group> groups = groupsUni.await().indefinitely();

        assertEquals(1, groups.size(), "The list should contain exactly one group");

        // Assert that the group name matches
        assertEquals("TestGroup", groups.get(0).getName(), "The group name should match");
    }

    @Test
    @Order(3)
    public void testAddUserToGroup() {
        Uni<User> user1Uni = userService.findByUsername("user1")
                .onItem().ifNull().failWith(new IllegalStateException("User 'user1' not found"));

        Uni<User> user2Uni = userService.findByUsername("user2")
                .onItem().ifNull().failWith(new IllegalStateException("User 'user2' not found"));

        Uni<List<Group>> groupsUni = user1Uni
                .onItem().transformToUni(user -> groupService.getGroupsOfUser(user.getId()));

        Group group = groupsUni
                .onItem().transformToUni(groups -> {
                    if (groups.isEmpty()) {
                        return Uni.createFrom().failure(new IllegalStateException("User 'user1' has no groups"));
                    }
                    return Uni.createFrom().item(groups.get(0));
                })
                .await().indefinitely();

        Uni<List<User>> usersInGroupUni = groupService.getGroupMembers(group.getId());

        List<User> usersInGroup = usersInGroupUni.await().indefinitely();

        groupService.addUserToGroup(user2Uni.await().indefinitely().getId(), group.getId());

        // Refresh the list of users in the group after adding a new user
        usersInGroup = groupService.getGroupMembers(group.getId()).await().indefinitely();

        assertEquals(1, usersInGroup.size());
        assertEquals(user2Uni.await().indefinitely().getId(), usersInGroup.get(0).getId());
    }


    @Test
    @Order(4)
    public void testAddExistingUserToGroup() {
        Uni<User> user1Uni = userService.findByUsername("user1")
                .onItem().ifNull().failWith(new IllegalStateException("User 'user1' not found"));

        Uni<User> user2Uni = userService.findByUsername("user2")
                .onItem().ifNull().failWith(new IllegalStateException("User 'user2' not found"));

        Uni<Group> groupUni = user1Uni
                .onItem().transformToUni(user -> {
                    return userService.findUserGroups(user.getId())
                            .onItem().ifNull().failWith(new IllegalStateException("User 'user1' has no groups"))
                            .onItem().transform(groups -> groups.get(0));
                });

        // Assert that adding an existing user to a group throws DuplicateResourceException
        Uni<Void> resultUni = Uni.combine().all().unis(user2Uni, groupUni)
                .asTuple()
                .onItem().transformToUni(tuple -> groupService.addUserToGroup(tuple.getItem1().getId(), tuple.getItem2().getId()));

        // Block and wait for the completion of the operation
        assertThrows(DuplicateResourceException.class, () -> resultUni.await().indefinitely());
    }

    @Test
    @Order(5)
    public void testAddMultipleUsersToGroup() {
        Uni<User> user1Uni = userService.findByUsername("user1")
                .onItem().ifNull().failWith(new IllegalStateException("User 'user1' not found"));

        Uni<User> user2Uni = userService.findByUsername("user2")
                .onItem().ifNull().failWith(new IllegalStateException("User 'user2' not found"));

        Uni<User> user3Uni = userService.createUser(new User("user3", "password3"))
                .onItem().transformToUni(user -> {
                    if (user == null) {
                        return Uni.createFrom().failure(new IllegalStateException("Failed to create user 'user3'"));
                    }
                    return Uni.createFrom().item(user);
                });

        Uni<Group> groupUni = user1Uni
                .onItem().transformToUni(user -> {
                    return userService.findUserGroups(user.getId())
                            .onItem().ifNull().failWith(new IllegalStateException("User 'user1' has no groups"))
                            .onItem().transform(groups -> groups.get(0));
                });

        // Add user3 to the group
        Uni<Void> resultUni = Uni.combine().all().unis(user3Uni, groupUni)
                .asTuple()
                .onItem().transformToUni(tuple -> groupService.addUserToGroup(tuple.getItem1().getId(), tuple.getItem2().getId()));

        // Block and wait for the completion of the operation
        resultUni.await().indefinitely();

        // Refresh the list of users in the group after adding a new user
        Uni<List<User>> usersFromGroupUni = groupUni
                .onItem().transformToUni(group -> groupService.getGroupMembers(group.getId()));

        List<User> usersFromGroup = usersFromGroupUni.await().indefinitely();

        assertEquals(2, usersFromGroup.size());
        assertEquals(user2Uni.await().indefinitely().getId(), usersFromGroup.get(0).getId());
        assertEquals(user3Uni.await().indefinitely().getId(), usersFromGroup.get(1).getId());
    }



    @Test
    @Order(6)
    public void testRemoveUserFromGroup() {
        Uni<User> user1Uni = userService.findByUsername("user1");
        Uni<User> user2Uni = userService.findByUsername("user2");
        Uni<Group> groupUni = user1Uni
                .onItem().transformToUni(user -> userService.findUserGroups(user.getId())
                        .onItem().ifNull().failWith(new IllegalStateException("User 'user1' has no groups"))
                        .onItem().transform(groups -> groups.isEmpty() ? null : groups.get(0)));

        User user1 = user1Uni.await().indefinitely();
        User user2 = user2Uni.await().indefinitely();
        Group group = groupUni.await().indefinitely();

        assertNotNull(group, "Group should not be null");

        groupService.removeUserFromGroup(user2.getId(), group.getId())
                .await().indefinitely();

        List<User> usersFromGroup = groupService.getGroupMembers(group.getId())
                .await().indefinitely();
        assertEquals(1, usersFromGroup.size(), "There should be only one user in the group");

        userService.removeUser(user2.getId());
    }

    @Test
    @Order(7)
    public void testRemoveGroup() {
        Uni<User> user3Uni = userService.findByUsername("user3");
        User user3 = user3Uni.await().indefinitely();
        groupService.createGroup(user3.getId(), "TestGroup2")
                .await().indefinitely();

        Uni<User> user1Uni = userService.findByUsername("user1");
        User user1 = user1Uni.await().indefinitely();
        Group group = user3Uni
                .onItem().transformToUni(user -> userService.findUserGroups(user.getId())
                        .onItem().ifNull().failWith(new IllegalStateException("User 'user3' has no groups"))
                        .onItem().transform(groups -> groups.isEmpty() ? null : groups.get(0)))
                .await().indefinitely();
        assertNotNull(group, "Group should not be null");

        user1Uni.await().indefinitely();
        groupService.addUserToGroup(user1.getId(), group.getId())
                .await().indefinitely();

        groupService.removeGroup(group.getId())
                .await().indefinitely();

        assertThrows(GroupNotFoundException.class, () -> userService.findUserGroups(user3.getId())
                .await().indefinitely(), "Group should not be found");

        assertThrows(GroupNotFoundException.class, () -> groupService.getGroupMembers(group.getId())
                .await().indefinitely(), "Group should not be found");
    }


    @Test
    @Order(8)
    public void testDeleteUser() {
        Uni<User> user1Uni = userService.findByUsername("user1");
        Uni<User> user3Uni = userService.findByUsername("user3");

        User user1 = user1Uni.await().indefinitely();
        User user3 = user3Uni.await().indefinitely();

        userService.removeUser(user3.getId())
                .await().indefinitely();

        assertNull(userService.findById(user3.getId()), "User should be deleted");

        List<Group> groups = userService.findUserGroups(user1.getId())
                .await().indefinitely();
        assertThrows(MembersNotFoundException.class, () -> groupService.getGroupMembers(groups.get(0).getId())
                .await().indefinitely(), "User should be removed from groups");

        userService.removeUser(user1.getId())
                .await().indefinitely();
    }




}
