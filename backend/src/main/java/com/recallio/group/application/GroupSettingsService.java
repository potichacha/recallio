package com.recallio.group.application;

import com.recallio.content.SubjectDirectory;
import com.recallio.group.GroupReviewScopes;
import com.recallio.group.application.StudyGroupService.GroupDetail;
import com.recallio.group.domain.GroupSettings;
import com.recallio.group.domain.GroupSettingsRepository;
import com.recallio.group.domain.InvalidGroupException;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Les réglages d'un groupe (CLAUDE.md, section 6.4 ; ADR 0017) : le créateur les modifie, et ils décident de ce que
 * « réviser avec le groupe » propose à ses membres.
 */
@ApplicationScoped
public class GroupSettingsService implements GroupReviewScopes {

    private final GroupSettingsRepository settings;
    private final GroupAccess access;
    private final StudyGroupService groupService;
    private final SubjectDirectory subjects;
    private final Clock clock;

    public GroupSettingsService(
            GroupSettingsRepository settings,
            GroupAccess access,
            StudyGroupService groupService,
            SubjectDirectory subjects,
            Clock clock) {
        this.settings = settings;
        this.access = access;
        this.groupService = groupService;
        this.subjects = subjects;
        this.clock = clock;
    }

    /**
     * Le créateur remplace les réglages du groupe.
     *
     * @throws InvalidGroupException si un sujet n'est pas publié, ou si un thème n'appartient à aucun des sujets choisis
     */
    @Transactional
    public GroupDetail update(UserRef user, UUID groupId, GroupSettings requested) {
        access.ownedGroup(user, groupId);
        requireKnownSubjectsAndThemes(requested);
        settings.save(groupId, requested, clock.instant());
        return groupService.group(user, groupId);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public GroupReviewScope of(UserRef user, UUID groupId) {
        access.visibleGroup(user, groupId);
        GroupSettings current = settings.findByGroup(groupId);
        return new GroupReviewScope(
                Set.copyOf(current.subjectSlugs()), Set.copyOf(current.themeSlugs()), current.cardTypes());
    }

    private void requireKnownSubjectsAndThemes(GroupSettings requested) {
        List<String> published = subjects.publishedSubjectSlugs();
        Set<String> allowedThemes = new HashSet<>();
        for (String subject : requested.subjectSlugs()) {
            if (!published.contains(subject)) {
                throw new InvalidGroupException("group.unknown-subject", "Le sujet « " + subject + " » n'existe pas.");
            }
            allowedThemes.addAll(subjects.themeSlugs(subject));
        }
        for (String theme : requested.themeSlugs()) {
            if (!allowedThemes.contains(theme)) {
                throw new InvalidGroupException(
                        "group.unknown-theme", "Le thème « " + theme + " » n'appartient à aucun sujet du groupe.");
            }
        }
    }
}
