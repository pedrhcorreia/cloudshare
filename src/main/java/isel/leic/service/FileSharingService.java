package isel.leic.service;

import isel.leic.exception.*;
import isel.leic.model.FileSharing;
import isel.leic.model.FileSharingResponse;
import isel.leic.model.Group;
import isel.leic.model.User;
import isel.leic.model.storage.FileObject;
import isel.leic.repository.FileSharingRepository;
import isel.leic.repository.GroupRepository;
import isel.leic.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class FileSharingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSharingService.class);

    @Inject
    FileSharingRepository fileSharingRepository;
    @Inject
    UserRepository userRepository;
    @Inject
    S3Client minioClient;

    @Inject
    GroupRepository groupRepository;

    @Transactional
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

    @Transactional
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

    @Transactional
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


    public List<FileSharingResponse> getFilesSharedByUser(Long userId) {
        LOGGER.info("Fetching files shared by user: {}", userId);

        User user = userRepository.findById(userId);
        if (user == null) {
            LOGGER.error("User with ID: {} not found", userId);
            throw new UserNotFoundException("User " + userId + " not found");
        }

        List<FileSharing> sharedFiles = fileSharingRepository.findBySharedByUserId(userId)
                .orElse(Collections.emptyList());

        List<FileSharingResponse> responseList = new ArrayList<>();
        for (FileSharing fileSharing : sharedFiles) {
            User sharedToUser = userRepository.findById(fileSharing.getSharedToUserId());
            if (sharedToUser != null) {
                fileSharing.setSharedToUsername(sharedToUser.getUsername());
            }

            List<FileObject> fileObjects = listObjectsFromBucket(fileSharing.getSharedByUserId(), fileSharing.getFilename());
            for (FileObject fileObject : fileObjects) {
                responseList.add(new FileSharingResponse(fileSharing, fileObject));
            }
        }

        return responseList;
    }

    public List<FileSharingResponse> getFilesSharedToUser(Long sharedToUserId) {
        LOGGER.info("Fetching files shared to user: {}", sharedToUserId);

        User user = userRepository.findById(sharedToUserId);
        if (user == null) {
            LOGGER.error("User with ID: {} not found", sharedToUserId);
            throw new UserNotFoundException("User with ID: " + sharedToUserId + " not found");
        }

        List<FileSharing> sharedFiles = fileSharingRepository.findBySharedToUserId(sharedToUserId)
                .orElse(Collections.emptyList());

        List<FileSharingResponse> responseList = new ArrayList<>();
        for (FileSharing fileSharing : sharedFiles) {
            User sharedByUser = userRepository.findById(fileSharing.getSharedByUserId());
            if (sharedByUser != null) {
                fileSharing.setSharedByUsername(sharedByUser.getUsername());
            }

            List<FileObject> fileObjects = listObjectsFromBucket(fileSharing.getSharedByUserId(), fileSharing.getFilename());
            for (FileObject fileObject : fileObjects) {
                responseList.add(new FileSharingResponse(fileSharing, fileObject));
            }
        }

        return responseList;
    }


    private List<FileObject> listObjectsFromBucket(Long userId, String filename) {
        String bucketName = userId + "-bucket";

        ListObjectsV2Request listObjectsReqManual = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(filename)
                .build();

        ListObjectsV2Response listObjResponse = minioClient.listObjectsV2(listObjectsReqManual);

        return listObjResponse.contents().stream()
                .map(FileObject::from)
                .collect(Collectors.toList());
    }


    public boolean isFileSharedWithUser(Long ownerId, Long userId, String filename) {
        List<FileSharing> sharedFiles = fileSharingRepository.findBySharedByUserId(ownerId)
                .orElse(Collections.emptyList());

        for (FileSharing fileSharing : sharedFiles) {
            if (fileSharing.getFilename().equals(filename) && fileSharing.getSharedToUserId().equals(userId)) {
                return false;
            }
        }

        return true;
    }
}
