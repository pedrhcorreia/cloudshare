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

    @Transient
    private String sharedByUsername;

    @Transient
    private String sharedToUsername;

    @Column(name = "filename", nullable = false)
    private String filename;


    public FileSharing() {
    }

    public FileSharing(Long sharedByUserId, Long sharedToUserId,  String filename) {
        this.sharedByUserId = sharedByUserId;
        this.sharedToUserId = sharedToUserId;

        this.filename = filename;
    }


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

    public void setSharedByUsername(String sharedByUsername){
        this.sharedByUsername = sharedByUsername;
    }
    public void setSharedToUsername(String sharedToUsername){
        this.sharedToUsername = sharedToUsername;
    }
    public String getSharedToUsername(){
        return  sharedToUsername;
    }
    public String getSharedByUsername(){
        return sharedByUsername;
    }
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}

