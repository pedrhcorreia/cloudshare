package isel.leic.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import isel.leic.model.FileSharing;
import jakarta.enterprise.context.ApplicationScoped;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class FileSharingRepository implements PanacheRepositoryBase<FileSharing, Long> {

    public Uni<Boolean> existsByUsersAndFilename(Long sharedByUserId, Long sharedToUserId, String filename) {
        return find("((sharedByUserId = ?1 AND sharedToUserId = ?2) OR (sharedByUserId = ?2 AND sharedToUserId = ?1)) " +
                "AND filename = ?3", sharedByUserId, sharedToUserId, filename)
                .singleResult()
                .map(Objects::nonNull);
    }

    public Uni<Optional<List<FileSharing>>> findBySharedByUserId(Long sharedByUserId) {
        return find("sharedByUserId", sharedByUserId)
                .list()
                .map(fileSharings -> fileSharings.isEmpty() ? Optional.empty() : Optional.of(fileSharings));
    }

    public Uni<Optional<List<FileSharing>>> findBySharedToUserId(Long sharedToUserId) {
        return find("sharedToUserId", sharedToUserId)
                .list()
                .map(fileSharings -> fileSharings.isEmpty() ? Optional.empty() : Optional.of(fileSharings));
    }

    public Uni<Optional<List<FileSharing>>> findBySharedToGroupId(Long sharedToGroupId) {
        return find("sharedToGroupId", sharedToGroupId)
                .list()
                .map(fileSharings -> fileSharings.isEmpty() ? Optional.empty() : Optional.of(fileSharings));
    }
}
