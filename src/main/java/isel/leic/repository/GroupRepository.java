package isel.leic.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import isel.leic.model.Group;
import isel.leic.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;

import java.util.List;

@ApplicationScoped
public class GroupRepository implements PanacheRepository<Group> {
    public boolean existsByCreatorIdAndName(Long creatorId, String name) {
        try {
            find("creatorId = ?1 and name = ?2", creatorId, name).singleResult();
            return true; // Group with the same creatorId and name exists
        } catch (NoResultException ex) {
            return false; // No group found with the same creatorId and name
        }
    }

    public List<Group> findByCreatorId(Long creatorId) {
        return list("creatorId", creatorId);
    }
}
