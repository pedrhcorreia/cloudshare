package isel.leic.repository;


import io.quarkus.hibernate.orm.panache.PanacheRepository;
import isel.leic.model.User;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {
    public Optional<User> findByUsername(String username) {
        User user = find("username", username).firstResult();
        return Optional.ofNullable(user);
    }
    public Optional<List<User>> findUsersByGroupId(Long groupId) {
        List<User> users = find("select u from User u join GroupMember gm on u.id = gm.userId where gm.groupId = ?1", groupId).list();
        return Optional.ofNullable(users.isEmpty() ? null : users);
    }
}
