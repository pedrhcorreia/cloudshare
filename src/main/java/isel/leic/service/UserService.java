package isel.leic.service;

import isel.leic.model.User;
import isel.leic.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;


import java.util.List;
import java.util.Optional;

@Transactional
@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    public User findByUsername(String username) {
        return userRepository.find("username", username).firstResult();
    }


    public boolean existsByUsername(String username) {
        return userRepository.find("username", username).firstResultOptional().isPresent();
    }
    public List<User> findAll() {
        return userRepository.listAll();
    }

    public void persist(User user) {
        userRepository.persist(user);
    }

    public void delete(User user) {
        userRepository.delete(user);
    }

    public Optional<User> authenticate(String username, String password) {
        Optional<User> optionalUser = userRepository.find("username", username).firstResultOptional();
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getPassword().equals(password)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }
}
