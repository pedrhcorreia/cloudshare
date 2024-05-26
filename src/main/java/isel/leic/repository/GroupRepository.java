package isel.leic.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import isel.leic.model.Group;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GroupRepository implements PanacheRepositoryBase<Group, Long> {

    public Uni<Boolean> existsByCreatorIdAndName(Long creatorId, String name) {
        return find("creatorId = ?1 and name = ?2", creatorId, name)
                .firstResult()
                .map(group -> group != null);
    }

    public Uni<List<Group>> findByCreatorId(Long creatorId) {
        return list("creatorId", creatorId);
    }


    public Uni<Optional<Group>> findByCreatorIdAndName(Long creatorId, String groupName) {
        return find("creatorId = ?1 and name = ?2", creatorId, groupName)
                .firstResult()
                .map(Optional::ofNullable);
    }
}
