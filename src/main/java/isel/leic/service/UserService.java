package isel.leic.service;

import io.smallrye.mutiny.Uni;
import isel.leic.exception.DuplicateResourceException;
import isel.leic.exception.GroupNotFoundException;
import isel.leic.exception.UserNotFoundException;
import isel.leic.model.Group;
import isel.leic.model.User;
import isel.leic.repository.GroupRepository;
import isel.leic.repository.UserRepository;
import isel.leic.utils.AuthorizationUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

@Transactional
@ApplicationScoped
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Inject
    UserRepository userRepository;

    @Inject
    GroupRepository groupRepository;

    public Uni<User> findById(Long id) {
        LOGGER.info("Fetching user by id: {}", id);
        return userRepository.findById(id)
                .onItem().ifNull().failWith(() -> new UserNotFoundException("User with ID " + id + " not found"));
    }

    public Uni<User> findByUsername(String username) {
        LOGGER.info("Fetching user by username: {}", username);
        return userRepository.findByUsername(username)
                .onItem().ifNull().failWith(() -> new UserNotFoundException("User with username " + username + " not found"));
    }

    public Uni<Boolean> existsById(Long id) {
        LOGGER.info("Checking if user exists with id: {}", id);
        return userRepository.findById(id)
                .onItem().transform(user -> user != null);
    }

    public Uni<List<User>> findAll() {
        LOGGER.info("Fetching all users");
        return userRepository.listAll()
                .invoke(users -> LOGGER.info("Fetched {} users", users.size()));
    }

    public Uni<User> updatePassword(Long userId, String password) {
        if (userId == null || password == null) {
            throw new IllegalArgumentException("User ID and password cannot be null");
        }

        return userRepository.findById(userId)
                .onItem().ifNull().failWith(() -> new UserNotFoundException("User with ID " + userId + " not found"))
                .onItem().transform(user -> {
                    user.setPassword(AuthorizationUtils.encodePassword(password));
                    userRepository.persist(user);
                    LOGGER.info("Updated password for user with ID {} ", user.getId());
                    return user;
                });
    }

    public Uni<User> createUser(User user) {
        LOGGER.info("Persisting user: {}", user.getUsername());
        user.setPassword(AuthorizationUtils.encodePassword(user.getPassword()));

        return userRepository.findByUsername(user.getUsername())
                .onItem().transformToUni(existingUser -> {
                    if (existingUser != null) {
                        LOGGER.error("User already exists: {}", user.getUsername());
                        throw new DuplicateResourceException("User already exists");
                    } else {
                        return userRepository.persistAndFlush(user)
                                .invoke(savedUser -> LOGGER.info("User persisted successfully: {}", savedUser.getUsername()));
                    }
                });
    }

    public Uni<Void> removeUser(Long userId) {
        LOGGER.info("Removing user: {}", userId);
        return userRepository.findById(userId)
                .onItem().ifNull().failWith(() -> new UserNotFoundException("User with ID " + userId + " not found"))
                .onItem().transformToUni(user -> userRepository.deleteById(userId)
                        .replaceWithVoid()
                        .invoke(() -> LOGGER.info("User {} removed successfully", userId))
                );
    }


    public Uni<User> authenticate(String username, String password) {
        LOGGER.info("Authenticating user: {}", username);

        return userRepository.findByUsername(username)
                .onItem().ifNull().failWith(() -> new UserNotFoundException("User not found - " + username))
                .onItem().transform(user -> {
                    if (AuthorizationUtils.verifyPassword(password, user.getPassword())) {
                        LOGGER.info("User {} authenticated successfully", username);
                        return user;
                    } else {
                        LOGGER.info("User {} authentication failed: Incorrect password", username);
                        throw new IllegalArgumentException("Incorrect password for user: " + username);
                    }
                });
    }

    public Uni<List<Group>> findUserGroups(Long userId) {
        LOGGER.info("Finding groups for user with id '{}'", userId);

        return userRepository.findById(userId)
                .onItem().ifNull().failWith(() -> new UserNotFoundException("User with ID " + userId + " not found"))
                .onItem().transformToUni(user -> {
                    return groupRepository.findByCreatorId(userId)
                            .onItem().transform(groups -> {
                                if (groups.isEmpty()) {
                                        LOGGER.info("No groups found for user with id '{}'", userId);
                                        return Collections.emptyList();
                                    } else {
                                        LOGGER.info("Found {} group(s) for user with id '{}'", groups.size(), userId);
                                        return groups;
                                    }

                            });
                });
    }
}