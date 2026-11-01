package com.recallio.group.infrastructure;

import com.recallio.group.domain.GroupInvitation;
import com.recallio.group.domain.GroupInvitationRepository;
import com.recallio.group.domain.InvitationToken;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaGroupInvitationRepository implements GroupInvitationRepository {

    private final EntityManager entityManager;

    public JpaGroupInvitationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void replace(GroupInvitation invitation) {
        // Un upsert : le lien précédent du groupe, s'il existe, est remplacé dans la même instruction.
        entityManager
                .createNativeQuery("""
                        insert into group_invitation (group_id, token, created_at, expires_at)
                        values (:groupId, :token, :createdAt, :expiresAt)
                        on conflict (group_id) do update
                        set token = excluded.token, created_at = excluded.created_at, expires_at = excluded.expires_at
                        """)
                .setParameter("groupId", invitation.groupId())
                .setParameter("token", invitation.token().value())
                .setParameter("createdAt", invitation.createdAt())
                .setParameter("expiresAt", invitation.expiresAt())
                .executeUpdate();
    }

    @Override
    public Optional<GroupInvitation> findByGroup(UUID groupId) {
        return Optional.ofNullable(entityManager.find(GroupInvitationEntity.class, groupId))
                .map(JpaGroupInvitationRepository::toDomain);
    }

    @Override
    public Optional<GroupInvitation> findByToken(InvitationToken token) {
        return entityManager
                .createQuery(
                        "select i from GroupInvitationEntity i where i.token = :token", GroupInvitationEntity.class)
                .setParameter("token", token.value())
                .getResultStream()
                .findFirst()
                .map(JpaGroupInvitationRepository::toDomain);
    }

    @Override
    public void deleteByGroup(UUID groupId) {
        entityManager
                .createQuery("delete from GroupInvitationEntity i where i.groupId = :groupId")
                .setParameter("groupId", groupId)
                .executeUpdate();
    }

    private static GroupInvitation toDomain(GroupInvitationEntity entity) {
        return new GroupInvitation(
                entity.groupId, new InvitationToken(entity.token), entity.createdAt, entity.expiresAt);
    }
}
