package isel.leic.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import isel.leic.model.GroupMember;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GroupMemberRepository implements PanacheRepository<GroupMember> {
    public Optional<List<Long>> findUsersByGroupId(Long groupId) {
        List<Long> userIds = find("groupId", groupId).stream().map(GroupMember::getUserId).toList();
        return Optional.ofNullable(userIds.isEmpty() ? null : userIds);
    }

    public Optional<List<GroupMember>> findByUserId(Long userId) {
        List<GroupMember> groupMembers = find("userId", userId).list();
        return Optional.ofNullable(groupMembers.isEmpty() ? null : groupMembers);
    }

    public void deleteByGroupIdAndUserId(Long groupId, Long userId) {
        delete("groupId = ?1 and userId = ?2", groupId, userId);
    }
}
