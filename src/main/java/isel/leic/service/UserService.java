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
    GroupMemberRepository groupMemberRepository;

    @Inject
    FileSharingRepository fileSharingRepository;

    @Inject
    GroupRepository groupRepository;

    public User findById(Long id) {
        LOGGER.info("Fetching user by id: {}", id);
        return userRepository.findById(id);
    }

    public User findByUsername(String username){
        return  userRepository.findByUsername(username);
    }

    public boolean existsById(Long id) {
        LOGGER.info("Checking if user exists with id: {}", id);
        return userRepository.findById(id) != null;
    }

    public List<User> findAll() {
        LOGGER.info("Fetching all users");
        return userRepository.listAll();
    }

    public List<FileSharing> getFilesSharedByUser(Long userId) {
        LOGGER.info("Fetching files shared by user: {}", userId);
        return fileSharingRepository.findBySharedByUserId(userId);
    }

    public List<FileSharing> getFilesSharedToUser(Long sharedToUsernameId) {
        LOGGER.info("Fetching files shared to user: {}", sharedToUsernameId);
        return fileSharingRepository.findBySharedToUserId(sharedToUsernameId);
    }

    public void createUser(User user) {
        LOGGER.info("Persisting user: {}", user.getUsername());
        userRepository.persist(user);
    }

    public void delete(User user) {
        LOGGER.info("Deleting user: {}", user.getId());
        /*for(GroupMember member: groupMemberRepository.findByUserId(user.getId())){
            groupMemberRepository.delete(member);
        }*/
        userRepository.delete(user);
        LOGGER.info("User {} deleted successfully", user.getId());
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



    public List<Group> findUserGroups(Long userId) {
        LOGGER.info("Finding groups for user with id '{}'", userId);

        User user = userRepository.findById(userId);
        if (user == null) {
            LOGGER.error("User with id '{}' not found", userId);
            return null;
        }

        List<Group> userGroups = groupRepository.findByCreatorId(userId);
        LOGGER.info("Found {} group(s) for user with id '{}'", userGroups.size(), userId);
        return userGroups;
    }


}
