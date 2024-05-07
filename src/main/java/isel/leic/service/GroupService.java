package isel.leic.service;

import isel.leic.exception.*;
import isel.leic.model.Group;
import isel.leic.model.GroupMember;
import isel.leic.model.User;
import isel.leic.repository.GroupMemberRepository;
import isel.leic.repository.GroupRepository;
import isel.leic.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Transactional
@ApplicationScoped
public class GroupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupService.class);

    @Inject
    GroupRepository groupRepository;
    @Inject
    UserRepository userRepository;
    @Inject
    GroupMemberRepository groupMemberRepository;

    public Optional<Group> findByCreatorIdAndName(Long creatorId, String name) {
        try {
            return groupRepository.findByCreatorIdAndName(creatorId,name);
        } catch (NoResultException ex) {
            throw new GroupNotFoundException("Group not found");
        }
    }
    public Group createGroup(Long creatorId, String groupName) throws IllegalArgumentException {

        if (creatorId == null || groupName == null) {
            LOGGER.warn("Invalid group object or parameters");
            throw new IllegalArgumentException("Invalid group object or parameters");
        }
        if(userRepository.findById(creatorId) == null){
            LOGGER.error("User with ID: {} not found", creatorId);
            throw new UserNotFoundException("User with ID '" + creatorId + "' not found");
        }
        LOGGER.info("Creating group '{}' for user with id '{}'", groupName, creatorId);
        if(groupRepository.existsByCreatorIdAndName(creatorId, groupName)){
            LOGGER.info("Group with name '{}' already exists for user with id '{}'", groupName, creatorId);
            throw new IllegalArgumentException("Group already exists for this user");
        }
        Group group =new Group(groupName,creatorId);
        groupRepository.persist(group);

        LOGGER.info("Group '{}' created successfully for user with id '{}'", groupName, creatorId);
        return group;
    }

    public void updateGroupName(Long groupId, String newName) {
        if (groupId == null || newName == null) {
            LOGGER.error("Group ID and new name cannot be null");
            throw new IllegalArgumentException("Group ID and new name cannot be null");
        }

        Group group = groupRepository.findById(groupId);
        if (group == null) {
            LOGGER.warn("Group with ID {} not found", groupId);
            throw new GroupNotFoundException("Group with ID " + groupId + " not found");
        }

        group.setName(newName);
        groupRepository.persist(group);
        LOGGER.info("Group name updated successfully for group with ID {}", groupId);
    }

    public void addUserToGroup(Long userId, Long groupId) {
        if (userId == null) {
            LOGGER.error("userId is null");
            throw new IllegalArgumentException("Invalid user ID: null");
        }

        User user = userRepository.findById(userId);
        if (user == null) {
            LOGGER.error("User with ID: {} not found", userId);
            throw new UserNotFoundException("User with ID '" + userId + "' not found");
        }

        Group group = groupRepository.findById(groupId);
        if (group == null) {
            LOGGER.error("Group with ID '{}' not found", groupId);
            throw new GroupNotFoundException("Group with ID '" + groupId + "' not found");
        }

        LOGGER.info("Adding user with ID '{}' to group with ID '{}'", userId, groupId);

        if (groupContainsUser(group, userId)) {
            LOGGER.warn("User with ID '{}' is already a member of group with ID '{}'", userId, groupId);
            throw new DuplicateResourceException("User with ID '" + userId + "' is already a member of group with ID '" + groupId + "'");
        }

        if (group.getCreatorId().equals(userId)) {
            LOGGER.warn("User with ID '{}' is the owner of group with ID '{}'", userId, groupId);
            throw new DuplicateResourceException("Cannot add group owner (ID '" + userId + "') to the group with ID '" + groupId + "'");
        }

        GroupMember groupMember = new GroupMember(userId, group.getId());
        groupMemberRepository.persist(groupMember);

        LOGGER.info("User with ID '{}' added to group with ID '{}'", userId, groupId);
    }



    private boolean groupContainsUser(Group group, Long userId) {
        Optional<List<User>> userListOptional = userRepository.findUsersByGroupId(group.getId());
        if (userListOptional.isPresent()) {
            List<User> userList = userListOptional.get();
            for (User member : userList) {
                if (member.getId().equals(userId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<User> getGroupMembers(Long groupId){
        LOGGER.info("Fetching members of group with id '{}'", groupId);

        Group group = groupRepository.findById(groupId);
        if (group == null) {
            LOGGER.error("Group with ID '{}' not found", groupId);
            throw new GroupNotFoundException("Group with ID '" + groupId + "' not found");
        }

        Optional<List<User>> groupUsersOptional = userRepository.findUsersByGroupId(groupId);

        if (groupUsersOptional.isPresent()) {
            List<User> groupUsersId = groupUsersOptional.get();
            LOGGER.info("Fetched {} members for group with id '{}'", groupUsersId.size(), groupId);
            return groupUsersId;
        } else {
            LOGGER.warn("No members found for group with id '{}'", groupId);
            throw new MembersNotFoundException("No members found for the group with ID: "+ groupId);
        }
    }


    public List<Group> getGroupsOfUser(Long userId) {
        LOGGER.info("Fetching groups of user with id '{}'", userId);

        Optional<List<Group>> userGroupsOptional = groupRepository.findByCreatorId(userId);
        List<Group> userGroups = userGroupsOptional.orElseThrow(() -> {
            LOGGER.error("No groups found for user with ID '{}'", userId);
            return new GroupNotFoundException("No groups found for user with ID '" + userId + "'");
        });

        LOGGER.info("Fetched {} groups for user with id '{}'", userGroups.size(), userId);
        return userGroups;
    }

    public void removeUserFromGroup(Long userId, Long groupId) {

        Group group = groupRepository.findById(groupId);
        if (group == null) {
            LOGGER.error("Group with ID '{}' not found", groupId);
            throw new GroupNotFoundException("Group with ID '" + groupId + "' not found");
        }

        if (!groupContainsUser(group, userId)) {
            LOGGER.warn("User with ID '{}' is not a member of group with ID '{}'", userId, groupId);
            throw new UserNotInGroupException("User with ID '" + userId + "' is not a member of group with ID '" + groupId + "'");
        }

        groupMemberRepository.deleteByGroupIdAndUserId(groupId, userId);
        LOGGER.info("User with ID '{}' removed from group with ID '{}'", userId, groupId);
    }


    public void removeGroup(Long id) throws IllegalArgumentException {
        LOGGER.info("Removing group with ID: {}", id);

        Optional<Group> existingGroupOptional = groupRepository.findByIdOptional(id);
        if (existingGroupOptional.isEmpty()) {
            LOGGER.warn("Group not found with ID: {}", id);
            throw new GroupNotFoundException("Group not found");
        }

        groupRepository.deleteById(id);
        LOGGER.info("Group with ID {} removed successfully", id);
    }



}
