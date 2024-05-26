package isel.leic.service;

import isel.leic.exception.*;
import isel.leic.model.FileSharing;
import isel.leic.model.Group;
import isel.leic.model.User;
import isel.leic.repository.FileSharingRepository;
import isel.leic.repository.GroupRepository;
import isel.leic.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class FileSharingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSharingService.class);

    @Inject
    FileSharingRepository fileSharingRepository;
    @Inject
    UserRepository userRepository;

    @Inject
    GroupRepository groupRepository;

    public FileSharing shareFileToUser(Long sharedByUsername, Long sharedToUsername, String filename) {
        LOGGER.info("Sharing file '{}' from user with ID: {} to user with ID: {}", filename, sharedByUsername, sharedToUsername);

        User sharedByUser = userRepository.findById(sharedByUsername);
        if (sharedByUser == null) {
            LOGGER.error("User with ID: {} not found", sharedByUsername);
            throw new UserNotFoundException("User with ID: " + sharedByUsername + " not found");
        }

        User sharedToUser = userRepository.findById(sharedToUsername);
        if (sharedToUser == null) {
            LOGGER.error("User with ID: {} not found", sharedToUsername);
            throw new UserNotFoundException("User with ID: " + sharedToUsername + " not found");
        }

        if (fileSharingRepository.existsByUsersAndFilename(sharedByUsername, sharedToUsername, filename)) {
            LOGGER.warn("File {} is already shared between users {} and {}", filename, sharedByUsername, sharedToUsername);
            throw new DuplicateResourceException("File '" + filename + "' is already shared between the users.");
        }

        FileSharing fileSharing = new FileSharing(sharedByUsername, sharedToUsername,  filename);
        fileSharingRepository.persist(fileSharing);
        LOGGER.info("File '{}' shared successfully from user {} to user {}", filename, sharedByUsername, sharedToUsername);
        return fileSharing;
    }

    public List<FileSharing> shareFileToGroup(Long sharedByUserId, Long sharedToGroupId, String filename) {
        LOGGER.info("Sharing file {} from user {} to group {}", filename, sharedByUserId, sharedToGroupId);

        Group sharedToGroup = groupRepository.findById(sharedToGroupId);
        if (sharedToGroup == null) {
            LOGGER.error("Group with ID: {} not found", sharedToGroupId);
            throw new GroupNotFoundException("Group with ID: " + sharedToGroupId + " not found");
        }

        Optional<List<User>> groupUsersOptional = userRepository.findUsersByGroupId(sharedToGroupId);
        if (groupUsersOptional.isEmpty()) {
            LOGGER.error("Group with ID: {} has no members", sharedToGroupId);
            throw new MembersNotFoundException("Group with ID: " + sharedToGroupId + " has no members");
        }

        List<User> usersInGroup = groupUsersOptional.get();
        List<FileSharing> fileSharings = new ArrayList<>();
        for (User user : usersInGroup) {
            FileSharing fileSharing = new FileSharing(sharedByUserId, user.getId(), filename);
            fileSharings.add(fileSharing);
        }

        fileSharingRepository.persist(fileSharings);
        LOGGER.info("File '{}' shared successfully from user {} to group {}", filename, sharedByUserId, sharedToGroupId);
        return fileSharings;
    }


    public void unshareFile(Long fileSharingId) {
        LOGGER.info("Unsharing file with ID {}", fileSharingId);

        FileSharing fileSharing = fileSharingRepository.findById(fileSharingId);

        if (fileSharing != null) {
            LOGGER.info("File sharing entry found with ID {}. Deleting...", fileSharingId);
            fileSharingRepository.delete(fileSharing);
            LOGGER.info("File sharing entry with ID {} deleted successfully", fileSharingId);
        } else {
            LOGGER.warn("File sharing entry with ID {} does not exist", fileSharingId);
            throw new FileSharingNotFoundException("File sharing entry with ID " + fileSharingId + " does not exist");
        }
    }

    public List<FileSharing> getFilesSharedByUser(Long userId) {
        LOGGER.info("Fetching files shared by user: {}", userId);

        User user = userRepository.findById(userId);
        if (user == null) {
            LOGGER.error("User with ID: {} not found", userId);
            throw new UserNotFoundException("User " + userId + " not found");
        }

        Optional<List<FileSharing>> sharedFilesOptional = fileSharingRepository.findBySharedByUserId(userId);
        return sharedFilesOptional.orElse(Collections.emptyList());
    }

    public List<FileSharing> getFilesSharedToUser(Long sharedToUserId) {
        LOGGER.info("Fetching files shared to user: {}", sharedToUserId);

        User user = userRepository.findById(sharedToUserId);
        if (user == null) {
            LOGGER.error("User with ID: {} not found", sharedToUserId);
            throw new UserNotFoundException("User with ID: " + sharedToUserId + " not found");
        }

        Optional<List<FileSharing>> sharedFilesOptional = fileSharingRepository.findBySharedToUserId(sharedToUserId);
        return sharedFilesOptional.orElse(Collections.emptyList());
    }


    public boolean isFileSharedWithUser(Long ownerId, Long userId, String filename) {
        List<FileSharing> sharedFiles = getFilesSharedByUser(ownerId);
        for (FileSharing fileSharing : sharedFiles) {
            if (fileSharing.getFilename().equals(filename) && fileSharing.getSharedToUserId().equals(userId)) {
                return true;
            }
        }

        return false;
    }
}
