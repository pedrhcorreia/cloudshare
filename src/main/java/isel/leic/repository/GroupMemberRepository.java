package isel.leic.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import isel.leic.model.GroupMember;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class GroupMemberRepository implements PanacheRepository<GroupMember> {
    public List<Long> findUsersByGroupId(Long groupId) {
        return find("groupId", groupId).stream().map(GroupMember::getUserId).toList();
    }
    public List<GroupMember> findByUserId(Long userId) {
        return find("userId", userId).list();
    }
    @Transactional
    public void deleteByGroupIdAndUserId(Long groupId, Long userId) {
        delete("groupId = ?1 and userId = ?2", groupId, userId);
    }
}
