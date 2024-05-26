package isel.leic.service;

import io.smallrye.mutiny.Uni;
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
import java.util.stream.Collectors;

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

    public Uni<FileSharing> shareFileToUser(Long sharedByUsername, Long sharedToUsername, String filename) {
        LOGGER.info("Sharing file '{}' from user with ID: {} to user with ID: {}", filename, sharedByUsername, sharedToUsername);

        return userRepository.findById(sharedByUsername)
                .onItem().ifNull().failWith(() -> new UserNotFoundException("User with ID: " + sharedByUsername + " not found"))
                .chain(sharedByUser -> userRepository.findById(sharedToUsername)
                        .onItem().ifNull().failWith(() -> new UserNotFoundException("User with ID: " + sharedToUsername + " not found"))
                        .chain(sharedToUser -> fileSharingRepository.existsByUsersAndFilename(sharedByUsername, sharedToUsername, filename)
                                .onItem().transformToUni(exists -> {
                                    if (exists) {
                                        LOGGER.warn("File {} is already shared between users {} and {}", filename, sharedByUsername, sharedToUsername);
                                        throw new DuplicateResourceException("File '" + filename + "' is already shared between the users.");
                                    } else {
                                        FileSharing fileSharing = new FileSharing(sharedByUsername, sharedToUsername, filename);
                                        return fileSharingRepository.persist(fileSharing)
                                                .onItem().transform(saved -> {
                                                    LOGGER.info("File '{}' shared successfully from user {} to user {}", filename, sharedByUsername, sharedToUsername);
                                                    return fileSharing;
                                                });
                                    }
                                })));
    }






    public Uni<List<FileSharing>> shareFileToGroup(Long sharedByUserId, Long sharedToGroupId, String filename) {
        LOGGER.info("Sharing file {} from user {} to group {}", filename, sharedByUserId, sharedToGroupId);

        return groupRepository.findById(sharedToGroupId)
                .onItem().ifNull().failWith(() -> new GroupNotFoundException("Group with ID: " + sharedToGroupId + " not found"))
                .chain(sharedToGroup -> userRepository.findUsersByGroupId(sharedToGroupId)
                        .onItem().ifNull().failWith(() -> new MembersNotFoundException("Group with ID: " + sharedToGroupId + " has no members"))
                        .onItem().transformToUni(groupUsers -> {
                            List<FileSharing> fileSharings = groupUsers.stream()
                                    .map(user -> new FileSharing(sharedByUserId, user.getId(), filename))
                                    .collect(Collectors.toList());
                            return fileSharingRepository.persist(fileSharings)
                                    .onItem().transform(saved -> {
                                        LOGGER.info("File '{}' shared successfully from user {} to group {}", filename, sharedByUserId, sharedToGroupId);
                                        return fileSharings;
                                    });
                        }));
    }



    public Uni<Void> unshareFile(Long fileSharingId) {
        LOGGER.info("Unsharing file with ID {}", fileSharingId);

        return fileSharingRepository.findById(fileSharingId)
                .onItem().ifNull().failWith(() -> {
                    LOGGER.warn("File sharing entry with ID {} does not exist", fileSharingId);
                    throw new FileSharingNotFoundException("File sharing entry with ID " + fileSharingId + " does not exist");
                })
                .onItem().invoke(fileSharing -> {
                    LOGGER.info("File sharing entry found with ID {}. Deleting...", fileSharingId);
                    fileSharingRepository.delete(fileSharing);
                    LOGGER.info("File sharing entry with ID {} deleted successfully", fileSharingId);
                }).replaceWithVoid();
    }




    public Uni<List<FileSharing>> getFilesSharedByUser(Long userId) {
        LOGGER.info("Fetching files shared by user: {}", userId);

        return userRepository.findById(userId)
                .onItem().ifNull().failWith(() -> {
                    LOGGER.error("User with ID: {} not found", userId);
                    throw new UserNotFoundException("User " + userId + " not found");
                })
                .onItem().transformToUni(user -> fileSharingRepository.findBySharedByUserId(userId))
                .onItem().transform(optional -> optional.orElse(Collections.emptyList()));
    }

    public Uni<List<FileSharing>> getFilesSharedToUser(Long sharedToUserId) {
        LOGGER.info("Fetching files shared to user: {}", sharedToUserId);

        return userRepository.findById(sharedToUserId)
                .onItem().ifNull().failWith(() -> {
                    LOGGER.error("User with ID: {} not found", sharedToUserId);
                    throw new UserNotFoundException("User with ID: " + sharedToUserId + " not found");
                })
                .onItem().transformToUni(user -> fileSharingRepository.findBySharedToUserId(sharedToUserId))
                .onItem().transform(optional -> optional.orElse(Collections.emptyList()));
    }


    public Uni<Boolean> isFileSharedWithUser(Long ownerId, Long userId, String filename) {
        LOGGER.info("Checking if file '{}' is shared with user {} by owner {}", filename, userId, ownerId);

        return getFilesSharedByUser(ownerId)
                .onItem().transform(sharedFiles -> {
                    for (FileSharing fileSharing : sharedFiles) {
                        if (fileSharing.getFilename().equals(filename) && fileSharing.getSharedToUserId().equals(userId)) {
                            return true;
                        }
                    }
                    return false;
                });
    }
}
