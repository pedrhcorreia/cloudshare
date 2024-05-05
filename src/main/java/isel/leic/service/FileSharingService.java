package isel.leic.service;

import isel.leic.exceptions.DuplicateFileSharingException;
import isel.leic.model.FileSharing;
import isel.leic.model.User;
import isel.leic.repository.FileSharingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Transactional
public class FileSharingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSharingService.class);

    @Inject
    FileSharingRepository fileSharingRepository;


    public void shareFileToUser(Long sharedByUsername, Long sharedToUsername, String filename) {
        LOGGER.info("Sharing file '{}' from user '{}' to user '{}'", filename, sharedByUsername, sharedToUsername);
        if (fileSharingRepository.existsByUsersAndFilename(sharedByUsername, sharedToUsername, filename)) {
            LOGGER.warn("File '{}' is already shared between users '{}' and '{}'", filename, sharedByUsername, sharedToUsername);
            throw new DuplicateFileSharingException("File '" + filename + "' is already shared between the users.");
        }
        FileSharing fileSharing = new FileSharing(sharedByUsername, sharedToUsername, null, filename);
        fileSharingRepository.persist(fileSharing);
        LOGGER.info("File '{}' shared successfully from user '{}' to user '{}'", filename, sharedByUsername, sharedToUsername);
    }

    public void shareFileToGroup(Long sharedByUsername, Long sharedToGroup, String filename) {
        LOGGER.info("Sharing file '{}' from user '{}' to group '{}'", filename, sharedByUsername, sharedToGroup);
        if (fileSharingRepository.existsByUsersAndFilename(sharedByUsername, sharedToGroup, filename)) {
            LOGGER.warn("File '{}' is already shared between user '{}' and group '{}'", filename, sharedByUsername, sharedToGroup);
            throw new DuplicateFileSharingException("File '" + filename + "' is already shared between the user and group.");
        }
        FileSharing fileSharing = new FileSharing(sharedByUsername, null, sharedToGroup, filename);
        fileSharingRepository.persist(fileSharing);
        LOGGER.info("File '{}' shared successfully from user '{}' to group '{}'", filename, sharedByUsername, sharedToGroup);
    }


    public void unshareFile(Long fileSharingId) {
        LOGGER.info("Unsharing file with ID {}", fileSharingId);
        // Find the file sharing entry by its ID
        FileSharing fileSharing = fileSharingRepository.findById(fileSharingId);
        // Check if the file sharing entry exists
        if (fileSharing != null) {
            LOGGER.info("File sharing entry found with ID {}. Deleting...", fileSharingId);
            // Delete the file sharing entry
            fileSharingRepository.delete(fileSharing);
            LOGGER.info("File sharing entry with ID {} deleted successfully", fileSharingId);
        } else {
            LOGGER.warn("File sharing entry with ID {} does not exist", fileSharingId);
            throw new IllegalArgumentException("File sharing entry with ID " + fileSharingId + " does not exist");
        }
    }
}
