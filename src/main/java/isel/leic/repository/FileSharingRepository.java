package isel.leic.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import isel.leic.model.FileSharing;
import isel.leic.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;


@ApplicationScoped
public class FileSharingRepository implements PanacheRepository<FileSharing> {

    public boolean existsByUsersAndFilename(User sharedByUsername, User sharedToUsername, String filename) {
        try {
            // Check if there's a file sharing entry with the same filename between the two users
            FileSharing result = find("((sharedByUser = ?1 AND sharedToUser = ?2) OR (sharedByUser = ?2 AND sharedToUser = ?1)) " +
                    "AND filename = ?3", sharedByUsername, sharedToUsername, filename)
                    .singleResult();
            return true; // Entry exists
        } catch (NoResultException e) {
            return false; // Entry doesn't exist
        }
    }
}