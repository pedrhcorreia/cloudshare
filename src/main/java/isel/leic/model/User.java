package isel.leic.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(nullable = false,unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @ManyToMany(mappedBy = "sharedByUser" ,fetch = FetchType.EAGER)
    private List<FileSharing> filesSharedByUser;

    @ManyToMany(mappedBy = "sharedToUser",  fetch = FetchType.EAGER)
    private List<FileSharing> filesSharedToUser;

    public User(){

    }

    public User(String username, String password){
        this.username = username;
        this.password = password;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<FileSharing> getFilesSharedByUser() {
        return filesSharedByUser;
    }

    public void setFilesSharedByUser(List<FileSharing> filesSharedByUser) {
        this.filesSharedByUser = filesSharedByUser;
    }

    public List<FileSharing> getFilesSharedToUser() {
        return filesSharedToUser;
    }

    public void setFilesSharedToUser(List<FileSharing> filesSharedToUser) {
        this.filesSharedToUser = filesSharedToUser;
    }

}
