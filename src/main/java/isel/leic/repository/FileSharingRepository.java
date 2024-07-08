package isel.leic.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import isel.leic.model.FileSharing;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;

import java.util.List;
import java.util.Optional;


@ApplicationScoped
public class FileSharingRepository implements PanacheRepository<FileSharing> {

    public boolean existsByUsersAndFilename(Long sharedByUserId, Long sharedToUserId, String filename) {
        try {

            FileSharing result = find("((sharedByUserId = ?1 AND sharedToUserId = ?2) OR (sharedByUserId = ?2 AND sharedToUserId = ?1)) " +
                    "AND filename = ?3", sharedByUserId, sharedToUserId, filename)
                    .singleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }


    public Optional<List<FileSharing>> findBySharedByUserId(Long sharedByUserId) {
        List<FileSharing> fileSharings = list("sharedByUserId", sharedByUserId);
        return Optional.ofNullable(fileSharings.isEmpty() ? null : fileSharings);
    }

    public Optional<List<FileSharing>> findBySharedToUserId(Long sharedToUserId) {
        List<FileSharing> fileSharings = list("sharedToUserId", sharedToUserId);
        return Optional.ofNullable(fileSharings.isEmpty() ? null : fileSharings);
    }

    public Optional<List<FileSharing>> findBySharedToGroupId(Long sharedToGroupId) {
        List<FileSharing> fileSharings = list("sharedToGroupId", sharedToGroupId);
        return Optional.ofNullable(fileSharings.isEmpty() ? null : fileSharings);
    }
    public Optional<List<FileSharing>> findBySharedByUserIdAndFilename(Long sharedByUserId, String currentFilename) {
        List<FileSharing> fileSharings = find("sharedByUserId = ?1 AND filename = ?2", sharedByUserId, currentFilename)
                    .list();
        return Optional.ofNullable(fileSharings.isEmpty() ? null : fileSharings);
    }
}