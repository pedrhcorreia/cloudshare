package isel.leic.model;


import jakarta.persistence.*;

@Entity
@Table(name = "file_sharing")
public class FileSharing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "shared_by_username", referencedColumnName = "username")
    private User sharedByUser;

    @ManyToOne
    @JoinColumn(name = "shared_to_username", referencedColumnName = "username")
    private User sharedToUser;

    @Column(name = "filename", nullable = false, unique = true)
    private String filename;
    // Constructors
    public FileSharing() {
    }

    public FileSharing(User sharedByUser, User sharedToUser, String filename) {
        this.sharedByUser = sharedByUser;
        this.sharedToUser = sharedToUser;
        this.filename = filename;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSharedByUsername() {
        return sharedByUser;
    }

    public void setSharedByUsername(User sharedByUser) {
        this.sharedByUser = sharedByUser;
    }

    public User getSharedToUsername() {
        return sharedToUser;
    }

    public void setSharedToUsername(User sharedToUser) {
        this.sharedToUser = sharedToUser;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
