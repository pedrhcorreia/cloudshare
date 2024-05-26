package isel.leic.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import isel.leic.model.GroupMember;
import jakarta.enterprise.context.ApplicationScoped;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GroupMemberRepository implements PanacheRepositoryBase<GroupMember, Long> {

    public Uni<Optional<List<Long>>> findUsersByGroupId(Long groupId) {
        return find("groupId", groupId)
                .list()
                .map(groupMembers -> {
                    List<Long> userIds = groupMembers.stream().map(GroupMember::getUserId).toList();
                    return userIds.isEmpty() ? Optional.empty() : Optional.of(userIds);
                });
    }

    public Uni<Optional<List<GroupMember>>> findByUserId(Long userId) {
        return find("userId", userId)
                .list()
                .map(groupMembers -> groupMembers.isEmpty() ? Optional.empty() : Optional.of(groupMembers));
    }

    public Uni<Void> deleteByGroupIdAndUserId(Long groupId, Long userId) {
        return delete("groupId = ?1 and userId = ?2", groupId, userId)
                .chain(() -> Uni.createFrom().voidItem());
    }
}
