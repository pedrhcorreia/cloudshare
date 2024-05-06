package isel.leic.model;

import jakarta.persistence.*;



@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;


    public Group(){

    }
    public Group(String name, Long creatorId) {
        this.name = name;
        this.creatorId = creatorId;
    }


    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }


    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public Long getId(){
        return id;
    }


}
