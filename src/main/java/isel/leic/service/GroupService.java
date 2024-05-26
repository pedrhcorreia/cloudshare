package isel.leic.service;

import io.smallrye.mutiny.Uni;
import isel.leic.exception.*;
import isel.leic.model.Group;
import isel.leic.model.GroupMember;
import isel.leic.model.User;
import isel.leic.repository.GroupMemberRepository;
import isel.leic.repository.GroupRepository;
import isel.leic.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GroupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupService.class);

    @Inject
    GroupRepository groupRepository;
    @Inject
    UserRepository userRepository;
    @Inject
    GroupMemberRepository groupMemberRepository;

    public Uni<Optional<Group>> findByCreatorIdAndName(Long creatorId, String name) {
        return groupRepository.findByCreatorIdAndName(creatorId, name);
    }

    public Uni<Group> createGroup(Long creatorId, String groupName) {
        return userRepository.findById(creatorId)
                .onItem().ifNull().failWith(() -> new UserNotFoundException("User with ID " + creatorId + " not found"))
                .chain(user -> {
                    LOGGER.info("Creating group '{}' for user with id '{}'", groupName, creatorId);
                    Group group = new Group(groupName, creatorId);
                    return groupRepository.existsByCreatorIdAndName(creatorId, groupName)
                            .onItem().transformToUni(exists -> {
                                if (exists) {
                                    throw new IllegalArgumentException("Group already exists for this user");
                                } else {
                                    return groupRepository.persist(group)
                                            .onItem().transform(savedGroup -> {
                                                LOGGER.info("Group '{}' created successfully for user with id '{}'", groupName, creatorId);
                                                return savedGroup;
                                            });
                                }
                            });
                });
    }
    public Uni<Group> updateGroupName(Long groupId, String newName) {
        return groupRepository.findById(groupId)
                .onItem().ifNull().failWith(() -> new GroupNotFoundException("Group with ID " + groupId + " not found"))
                .chain(group -> {
                    LOGGER.info("Updating group name to '{}' for group with ID '{}'", newName, groupId);
                    group.setName(newName);
                    return groupRepository.persist(group)
                            .onItem().transform(savedGroup -> {
                                LOGGER.info("Group name updated successfully for group with ID '{}'", groupId);
                                return savedGroup;
                            });
                });
    }


    public Uni<Void> addUserToGroup(Long userId, Long groupId) {
        return userRepository.findById(userId)
                .onItem().ifNull().failWith(() -> new UserNotFoundException("User with ID " + userId + " not found"))
                .onItem().transformToUni(user -> groupRepository.findById(groupId)
                        .onItem().ifNull().failWith(() -> new GroupNotFoundException("Group with ID " + groupId + " not found"))
                        .onItem().transformToUni(group -> {
                            LOGGER.info("Adding user with ID '{}' to group with ID '{}'", userId, groupId);
                            if (group.getCreatorId().equals(userId)) {
                                throw new DuplicateResourceException("Cannot add group owner (ID '" + userId + "') to the group with ID '" + groupId + "'");
                            }
                            return groupMemberRepository.findByUserId(userId)
                                    .onItem().transformToUni(groupMembersOptional -> {
                                        if (groupMembersOptional.isPresent()) {
                                            List<GroupMember> groupMembers = groupMembersOptional.get();
                                            for (GroupMember member : groupMembers) {
                                                if (member.getGroupId().equals(groupId)) {
                                                    throw new DuplicateResourceException("User with ID '" + userId + "' is already a member of group with ID '" + groupId + "'");
                                                }
                                            }
                                        }
                                        GroupMember groupMember = new GroupMember(userId, groupId);
                                        return groupMemberRepository.persist(groupMember).replaceWithVoid();
                                    });
                        }));
    }


    public Uni<List<User>> getGroupMembers(Long groupId) {
        LOGGER.info("Fetching members of group with id '{}'", groupId);
        return groupRepository.findById(groupId)
                .onItem().ifNull().failWith(() -> new GroupNotFoundException("Group with ID " + groupId + " not found"))
                .onItem().transformToUni(group -> {
                    return userRepository.findUsersByGroupId(groupId)
                            .onItem().transform(users -> {
                                LOGGER.info("Fetched {} members for group with id '{}'", users.size(), groupId);
                                return users;
                            });
                });
    }

    public Uni<List<Group>> getGroupsOfUser(Long userId) {
        LOGGER.info("Fetching groups of user with id '{}'", userId);
        return groupRepository.findByCreatorId(userId)
                .onItem().transform(groups -> {
                    LOGGER.info("Fetched {} groups for user with id '{}'", groups.size(), userId);
                    return groups;
                });
    }

    public Uni<Void> removeUserFromGroup(Long userId, Long groupId) {
        return userRepository.findById(userId)
                .onItem().ifNull().failWith(() -> new UserNotFoundException("User with ID " + userId + " not found"))
                .onItem().transformToUni(user -> {
                    return groupRepository.findById(groupId)
                            .onItem().ifNull().failWith(() -> new GroupNotFoundException("Group with ID " + groupId + " not found"))
                            .onItem().transformToUni(group -> {
                                return groupMemberRepository.deleteByGroupIdAndUserId(groupId, userId)
                                        .onItem().invoke(ignore -> LOGGER.info("User with ID '{}' removed from group with ID '{}'", userId, groupId));
                            });
                }).replaceWithVoid();
    }

    public Uni<Void> removeGroup(Long id) {
        LOGGER.info("Removing group with ID: {}", id);
        return groupRepository.findById(id)
                .onItem().ifNull().failWith(() -> new GroupNotFoundException("Group not found with ID: " + id))
                .onItem().transformToUni(group -> groupRepository.deleteById(group.getId()))
                .replaceWithVoid();
    }

}
