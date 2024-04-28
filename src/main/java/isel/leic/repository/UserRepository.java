package isel.leic.repository;


import io.quarkus.hibernate.orm.panache.PanacheRepository;
import isel.leic.model.User;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {
}
