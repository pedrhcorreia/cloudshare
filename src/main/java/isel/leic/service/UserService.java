package isel.leic.service;

import isel.leic.model.FileSharing;
import isel.leic.model.User;
import isel.leic.repository.FileSharingRepository;
import isel.leic.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Transactional
@ApplicationScoped
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Inject
    UserRepository userRepository;

    @Inject
    FileSharingService fileSharingService;

    public User findByUsername(String username) {
        LOGGER.info("Fetching user by username: {}", username);
        return userRepository.findByUsername(username);
    }

    public boolean existsByUsername(String username) {
        LOGGER.info("Checking if user exists with username: {}", username);
        return userRepository.findByUsername(username) != null;
    }

    public List<User> findAll() {
        LOGGER.info("Fetching all users");
        return userRepository.listAll();
    }

    public List<FileSharing> getFilesSharedByUser(String sharedByUsername) {
        LOGGER.info("Fetching files shared by user: {}", sharedByUsername);
        return findByUsername(sharedByUsername).getFilesSharedByUser();
    }

    public List<FileSharing> getFilesSharedToUser(String sharedToUsername) {
        LOGGER.info("Fetching files shared to user: {}", sharedToUsername);
        return findByUsername(sharedToUsername).getFilesSharedToUser();
    }

    public void persist(User user) {
        LOGGER.info("Persisting user: {}", user.getUsername());
        userRepository.persist(user);
    }

    public void delete(User user) {
        LOGGER.info("Deleting user: {}", user.getUsername());

        List<FileSharing> fileSharingList = user.getFilesSharedByUser();
        if (fileSharingList != null) {
            fileSharingList.forEach(fileSharing -> {
                LOGGER.info("Unsharing file {} associated with user {}", fileSharing.getFilename(), user.getUsername());
                fileSharingService.unshareFile(fileSharing.getId());
            });
        }

        List<FileSharing> fileSharingListSharedWithUser = user.getFilesSharedToUser();
        if (fileSharingListSharedWithUser != null) {
            fileSharingListSharedWithUser.forEach(fileSharing -> {
                LOGGER.info("Unsharing file {} shared with user {}", fileSharing.getFilename(), user.getUsername());
                fileSharingService.unshareFile(fileSharing.getId());
            });
        }

        userRepository.delete(user);
        LOGGER.info("User {} deleted successfully", user.getUsername());
    }

    public Optional<User> authenticate(String username, String password) {
        LOGGER.info("Authenticating user: {}", username);

        User user = userRepository.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            LOGGER.info("User {} authenticated successfully", username);
            return Optional.of(user);
        }

        LOGGER.info("User {} authentication failed", username);
        return Optional.empty();
    }
}
