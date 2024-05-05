package isel.leic.model;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "group_member")
@IdClass(GroupMemberId.class)
public class GroupMember implements Serializable {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "group_id")
    private Long groupId;

    public GroupMember() {}

    public GroupMember(Long userId, Long groupId) {
        this.userId = userId;
        this.groupId = groupId;
    }

    // Getters and setters

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
}
