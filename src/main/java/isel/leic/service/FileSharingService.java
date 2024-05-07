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

    public List<FileSharing> shareFileToGroup(Long sharedByUsername, Long sharedToGroup, String filename) {
        LOGGER.info("Sharing file {} from user {} to group {}", filename, sharedByUsername, sharedToGroup);

        Group sharedToGroupObj = groupRepository.findById(sharedToGroup);
        if (sharedToGroupObj == null) {
            LOGGER.error("Group with ID: {} not found", sharedToGroup);
            throw new GroupNotFoundException("Group with ID: " + sharedToGroup + " not found");
        }
        Optional<List<User>> groupUsersOptional = userRepository.findUsersByGroupId(sharedToGroup);
        List<FileSharing> fileSharings = null;
        if(groupUsersOptional.isEmpty()){
            LOGGER.error("Group with ID: {} has no members", sharedToGroup);
            throw new MembersNotFoundException("Group with ID: "+ sharedToGroup +"has no members");
        }else{
            List<User> usersInGroup  = groupUsersOptional.get();
            fileSharings = usersInGroup.stream()
                    .map(user -> shareFileToUser(sharedByUsername, user.getId(), filename))
                    .toList();
        }

        LOGGER.info("File '{}' shared successfully from user {} to group {}", filename, sharedByUsername, sharedToGroup);
        return fileSharings;
        //This method is an abomination please look at it later TODO
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
        if (sharedFilesOptional.isPresent()) {
            return sharedFilesOptional.get();
        } else {
            throw new FileSharingNotFoundException("No files found for user: " + userId);
        }
    }

    public List<FileSharing> getFilesSharedToUser(Long sharedToUserId) {
        LOGGER.info("Fetching files shared to user: {}", sharedToUserId);

        User user = userRepository.findById(sharedToUserId);
        if (user == null) {
            LOGGER.error("User with ID: {} not found", sharedToUserId);
            throw new UserNotFoundException("User with ID: " + sharedToUserId + " not found");
        }

        Optional<List<FileSharing>> sharedFilesOptional = fileSharingRepository.findBySharedToUserId(sharedToUserId);
        if (sharedFilesOptional.isPresent()) {
            return sharedFilesOptional.get();
        } else {
            throw new FileSharingNotFoundException("No files found shared to user with ID: " + sharedToUserId);
        }
    }
}
