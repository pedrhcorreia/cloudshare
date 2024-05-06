package isel.leic.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import isel.leic.model.Group;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;

import java.util.List;
import java.util.Optional;

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

    public Optional<List<Group>> findByCreatorId(Long creatorId) {
        List<Group> groups = list("creatorId", creatorId);
        return Optional.ofNullable(groups.isEmpty() ? null : groups);
    }

    public Optional<Group> findByCreatorIdAndName(Long creatorId, String groupName) {
        try {
            Group group = find("creatorId = ?1 and name = ?2", creatorId, groupName).singleResult();
            return Optional.of(group);
        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }
}
