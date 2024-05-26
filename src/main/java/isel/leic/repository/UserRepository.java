package isel.leic.repository;


import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import isel.leic.model.User;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;


@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {
    public Uni<User> findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public Uni<List<User>> findUsersByGroupId(Long groupId) {
        return find("select u from User u join GroupMember gm on u.id = gm.userId where gm.groupId = ?1", groupId).list();
    }

}
