package com.recallio.group.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.group.domain.GroupExceptions.OwnerCannotLeaveException;
import com.recallio.group.domain.GroupExceptions.OwnerOnlyException;
import com.recallio.group.domain.StudyGroup.Member;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StudyGroupTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    private final UUID owner = UUID.randomUUID();
    private final UUID member = UUID.randomUUID();
    private final StudyGroup group = new StudyGroup(
            UUID.randomUUID(),
            new GroupName("Équipe backend"),
            NOW,
            List.of(new Member(owner, GroupRole.OWNER, NOW), new Member(member, GroupRole.MEMBER, NOW)));

    @Test
    void shouldMakeTheCreatorTheOwnerAndOnlyMember() {
        StudyGroup created = StudyGroup.create(UUID.randomUUID(), new GroupName("Java"), owner, NOW);

        assertThat(created.members()).containsExactly(new Member(owner, GroupRole.OWNER, NOW));
        assertThat(created.isOwner(owner)).isTrue();
    }

    @Test
    void shouldRequireExactlyOneOwner() {
        var twoMembers = List.of(new Member(owner, GroupRole.MEMBER, NOW), new Member(member, GroupRole.MEMBER, NOW));

        assertThatThrownBy(() -> new StudyGroup(UUID.randomUUID(), new GroupName("Sans créateur"), NOW, twoMembers))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldLetOnlyTheOwnerManageTheGroup() {
        assertThatNoException().isThrownBy(() -> group.requireOwner(owner));
        assertThatThrownBy(() -> group.requireOwner(member)).isInstanceOf(OwnerOnlyException.class);
        assertThatThrownBy(() -> group.requireOwner(UUID.randomUUID())).isInstanceOf(OwnerOnlyException.class);
    }

    @Test
    void shouldLetMembersLeaveButNotTheOwner() {
        assertThatNoException().isThrownBy(() -> group.requireRemovable(member));
        assertThatThrownBy(() -> group.requireRemovable(owner)).isInstanceOf(OwnerCannotLeaveException.class);
    }

    @Test
    void shouldTrimTheNameAndRejectAnEmptyOrTooLongOne() {
        assertThat(new GroupName("  Équipe  ").value()).isEqualTo("Équipe");
        assertThatThrownBy(() -> new GroupName("   ")).isInstanceOf(InvalidGroupException.class);
        assertThatThrownBy(() -> new GroupName("x".repeat(GroupName.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidGroupException.class);
    }
}
