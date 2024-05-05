package isel.leic.model;

import jakarta.persistence.*;

@Entity
@Table(name = "file_sharing")
public class FileSharing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "shared_by_user_id")
    private Long sharedByUserId;

    @Column(name = "shared_to_user_id")
    private Long sharedToUserId;

    @Column(name = "shared_to_group_id")
    private Long sharedToGroupId;

    @Column(name = "filename", nullable = false)
    private String filename;

    // Constructors
    public FileSharing() {
    }

    public FileSharing(Long sharedByUserId, Long sharedToUserId, Long sharedToGroupId, String filename) {
        this.sharedByUserId = sharedByUserId;
        this.sharedToUserId = sharedToUserId;
        this.sharedToGroupId = sharedToGroupId;
        this.filename = filename;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSharedByUserId() {
        return sharedByUserId;
    }

    public void setSharedByUserId(Long sharedByUserId) {
        this.sharedByUserId = sharedByUserId;
    }

    public Long getSharedToUserId() {
        return sharedToUserId;
    }

    public void setSharedToUserId(Long sharedToUserId) {
        this.sharedToUserId = sharedToUserId;
    }

    public Long getSharedToGroupId() {
        return sharedToGroupId;
    }

    public void setSharedToGroupId(Long sharedToGroupId) {
        this.sharedToGroupId = sharedToGroupId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}

