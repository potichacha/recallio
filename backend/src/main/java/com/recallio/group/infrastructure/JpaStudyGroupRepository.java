package com.recallio.group.infrastructure;

import com.recallio.group.domain.GroupName;
import com.recallio.group.domain.GroupRole;
import com.recallio.group.domain.StudyGroup;
import com.recallio.group.domain.StudyGroup.Member;
import com.recallio.group.domain.StudyGroupRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Entités JPA séparées du domaine : le groupe et ses règles vivent dans {@link StudyGroup}. */
@ApplicationScoped
public class JpaStudyGroupRepository implements StudyGroupRepository {

    private final EntityManager entityManager;

    public JpaStudyGroupRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void create(StudyGroup group) {
        entityManager.persist(new StudyGroupEntity(group.id(), group.name().value(), group.createdAt()));
        // Le groupe doit exister en base avant ses adhésions (clé étrangère).
        entityManager.flush();
        group.members()
                .forEach(member -> entityManager.persist(
                        new GroupMemberEntity(group.id(), member.userId(), member.role(), member.joinedAt())));
    }

    @Override
    public Optional<StudyGroup> findById(UUID groupId) {
        StudyGroupEntity group = entityManager.find(StudyGroupEntity.class, groupId);
        if (group == null) {
            return Optional.empty();
        }
        List<Member> members = entityManager
                .createQuery("""
                        select m from GroupMemberEntity m
                        where m.groupId = :groupId
                        order by case when m.role = :owner then 0 else 1 end, m.joinedAt, m.userId
                        """, GroupMemberEntity.class)
                .setParameter("groupId", groupId)
                .setParameter("owner", GroupRole.OWNER)
                .getResultStream()
                .map(member -> new Member(member.userId, member.role, member.joinedAt))
                .toList();
        return Optional.of(new StudyGroup(groupId, new GroupName(group.name), group.createdAt, members));
    }

    @Override
    public List<GroupSummary> findByMember(UUID userId) {
        return entityManager
                .createQuery("""
                        select g.id, g.name, m.role,
                               (select count(other) from GroupMemberEntity other where other.groupId = g.id)
                        from GroupMemberEntity m
                        join StudyGroupEntity g on g.id = m.groupId
                        where m.userId = :userId
                        order by m.joinedAt desc, g.id
                        """, Object[].class)
                .setParameter("userId", userId)
                .getResultStream()
                .map(row -> new GroupSummary(
                        (UUID) row[0], new GroupName((String) row[1]), (GroupRole) row[2], ((Long) row[3]).intValue()))
                .toList();
    }

    @Override
    public long countOwnedBy(UUID userId) {
        return entityManager
                .createQuery(
                        "select count(m) from GroupMemberEntity m where m.userId = :userId and m.role = :owner",
                        Long.class)
                .setParameter("userId", userId)
                .setParameter("owner", GroupRole.OWNER)
                .getSingleResult();
    }

    @Override
    public void removeMember(UUID groupId, UUID userId) {
        entityManager
                .createQuery("delete from GroupMemberEntity m where m.groupId = :groupId and m.userId = :userId")
                .setParameter("groupId", groupId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Override
    public void delete(UUID groupId) {
        entityManager
                .createQuery("delete from GroupMemberEntity m where m.groupId = :groupId")
                .setParameter("groupId", groupId)
                .executeUpdate();
        entityManager
                .createQuery("delete from StudyGroupEntity g where g.id = :groupId")
                .setParameter("groupId", groupId)
                .executeUpdate();
    }
}
