package isel.leic.model;

import java.io.Serializable;
import java.util.Objects;

public class GroupMemberId implements Serializable {

    private Long userId;
    private Long groupId;

    public GroupMemberId() {}

    public GroupMemberId(Long userId, Long groupId) {
        this.userId = userId;
        this.groupId = groupId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupMemberId that = (GroupMemberId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, groupId);
    }
}