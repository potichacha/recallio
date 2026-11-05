package com.recallio.gamification.application;

import com.recallio.content.SubjectDirectory;
import com.recallio.gamification.application.AchievementService.AchievementView;
import com.recallio.gamification.domain.ActivityRepository;
import com.recallio.gamification.domain.PublicProfileNotFoundException;
import com.recallio.gamification.domain.Streak;
import com.recallio.gamification.domain.SubjectLevel;
import com.recallio.gamification.domain.XpLedger;
import com.recallio.gamification.domain.XpLedger.XpTotals;
import com.recallio.ranked.RankedProfiles;
import com.recallio.ranked.RankedProfiles.PublicRank;
import com.recallio.review.SkillProfiles;
import com.recallio.review.SkillProfiles.SubjectSkills;
import com.recallio.user.FollowedSubjects;
import com.recallio.user.PublicProfiles;
import com.recallio.user.PublicProfiles.PublicUser;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Cas d'usage : le profil public de compétences d'un utilisateur (ADR 0043), s'il l'a rendu public. Il assemble ce que
 * chaque module sait : le nom et l'adresse (user), le vérifié et le déclaré par sujet (review), l'XP, la série et les
 * succès (gamification).
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class PublicSkillProfileService {

    private final PublicProfiles publicProfiles;
    private final FollowedSubjects followedSubjects;
    private final SkillProfiles skills;
    private final RankedProfiles ranked;
    private final SubjectDirectory subjects;
    private final XpLedger ledger;
    private final ActivityRepository activity;
    private final AchievementService achievements;
    private final Clock clock;

    public PublicSkillProfileService(
            PublicProfiles publicProfiles,
            FollowedSubjects followedSubjects,
            SkillProfiles skills,
            RankedProfiles ranked,
            SubjectDirectory subjects,
            XpLedger ledger,
            ActivityRepository activity,
            AchievementService achievements,
            Clock clock) {
        this.publicProfiles = publicProfiles;
        this.followedSubjects = followedSubjects;
        this.skills = skills;
        this.ranked = ranked;
        this.subjects = subjects;
        this.ledger = ledger;
        this.activity = activity;
        this.achievements = achievements;
        this.clock = clock;
    }

    /** @throws PublicProfileNotFoundException si personne n'a cette adresse, ou si son profil est privé */
    public PublicSkillProfile profile(String handle) {
        PublicUser owner = publicProfiles.visible(handle).orElseThrow(() -> new PublicProfileNotFoundException(handle));
        UserRef user = owner.user();
        Map<String, String> titles = subjects.publishedTitles();
        XpTotals xp = ledger.totals(user.id());
        Map<String, PublicRank> ranks = ranked.ranks(user.id());
        List<SubjectProfile> perSubject = skills.of(user.id(), followedSubjects.of(user)).stream()
                .map(skill -> new SubjectProfile(
                        titles.getOrDefault(skill.subjectSlug(), skill.subjectSlug()),
                        SubjectLevel.of(xp.of(skill.subjectSlug())),
                        skill,
                        ranks.get(skill.subjectSlug())))
                .toList();
        LocalDate today = LocalDate.now(clock.withZone(user.timezone()));
        List<AchievementView> unlocked = achievements.all(user).stream()
                .filter(achievement -> achievement.unlockedAt() != null)
                .toList();
        return new PublicSkillProfile(
                owner.handle(),
                owner.displayName(),
                Streak.of(activity.goalDays(user.id()), today).days(),
                perSubject,
                unlocked);
    }

    /**
     * @param subjects les sujets suivis, dans l'ordre du catalogue
     * @param achievements les succès débloqués seulement
     */
    public record PublicSkillProfile(
            String handle,
            String displayName,
            int streak,
            List<SubjectProfile> subjects,
            List<AchievementView> achievements) {}

    /** @param rank le rang en classé, vérifié par le serveur ; absent pendant le placement, ou sans duel */
    public record SubjectProfile(
            String title,
            SubjectLevel level,
            SubjectSkills skills,
            @Nullable PublicRank rank) {}
}
