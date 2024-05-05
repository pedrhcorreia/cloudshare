package isel.leic.service;

import isel.leic.model.FileSharing;
import isel.leic.model.Group;
import isel.leic.model.GroupMember;
import isel.leic.model.User;
import isel.leic.repository.FileSharingRepository;
import isel.leic.repository.GroupMemberRepository;
import isel.leic.repository.GroupRepository;
import isel.leic.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
    @Inject
    FileSharingRepository fileSharingRepository;
    public void createGroup(Group group) {
        LOGGER.info("Creating group '{}' for user with id '{}'", group.getName(), group.getCreatorId());
        if(groupRepository.existsByCreatorIdAndName(group.getCreatorId(), group.getName())){
            LOGGER.info("Group with name'{}'already exists for user with id '{}'", group.getName(), group.getCreatorId());
            return;
        }

        groupRepository.persist(group);

        LOGGER.info("Group '{}' created successfully for user with id '{}'", group.getName(), group.getCreatorId());

    }
    public void addUserToGroup(Long userId, Long groupId) {
        LOGGER.info("Adding user with id '{}' to group with id '{}'", userId, groupId);

        Group group = groupRepository.findById(groupId);
        if (group == null) {
            LOGGER.error("Group with id '{}' not found", groupId);
            return;
        }

        // Check if the user is already a member of the group
        if (groupContainsUser(group, userId)) {
            LOGGER.warn("User with id '{}' is already a member of group with id '{}'", userId, groupId);
            return;
        }

        // Check if the user is the owner of the group
        if (group.getCreatorId().equals(userId)) {
            LOGGER.warn("User with id '{}' is the owner of group with id '{}'", userId, groupId);
            return;
        }

        // Create a new GroupMember entity to represent the relationship
        GroupMember groupMember = new GroupMember(userId, group.getId());
        groupMemberRepository.persist(groupMember);

        LOGGER.info("User with id '{}' added to group with id '{}'", userId, groupId);
    }


    private boolean groupContainsUser(Group group, Long userId) {
        List<User> userList = userRepository.findUsersByGroupId(group.getId());
        for (User member : userList) {
            if (member.getId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    public List<User> getGroupMember(Long groupId){
        LOGGER.info("Fetching members of group with id '{}'", groupId);
        List<User> groupUsersId = userRepository.findUsersByGroupId(groupId);
        LOGGER.info("Fetched {} members for group with id '{}'", groupUsersId.size(), groupId);
        return groupUsersId;
    }

    public List<Group> getGroupsOfUser(Long userId){
        LOGGER.info("Fetching groups of user with id '{}'", userId);
        List<Group> userGroups = groupRepository.findByCreatorId(userId);
        LOGGER.info("Fetched {} groups for user with id '{}'", userGroups.size(), userId);
        return userGroups;
    }

    public void removeUserFromGroup(Long userId, Long groupId) {
        groupMemberRepository.deleteByGroupIdAndUserId(groupId,userId);
    }

    public void removeGroup(Long id) {
        LOGGER.info("Removing group with ID: {}", id);
        groupRepository.deleteById(id);
        LOGGER.info("Group with ID {} removed successfully", id);
    }

    public List<FileSharing> getFilesSharedToGroup(Long groupId) {
        LOGGER.info("Fetching files shared to group: {}", groupId);
        return fileSharingRepository.findBySharedToGroupId(groupId);
    }
}
