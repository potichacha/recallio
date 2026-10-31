package com.recallio.content.domain.importing;

import com.recallio.content.domain.CardContent;
import com.recallio.content.domain.CardIdentity;
import com.recallio.content.domain.Subject;
import com.recallio.content.domain.importing.CatalogSnapshot.ExistingCard;
import com.recallio.content.domain.importing.CatalogSnapshot.ExistingTheme;
import com.recallio.content.domain.importing.ImportPlan.CardPlacement;
import com.recallio.content.domain.importing.ImportPlan.NewCard;
import com.recallio.content.domain.importing.ImportPlan.NewVersion;
import com.recallio.content.domain.importing.ImportPlan.SubjectUpsert;
import com.recallio.content.domain.importing.ImportPlan.ThemeUpsert;
import com.recallio.content.domain.importing.ImportReport.Duplicate;
import com.recallio.content.domain.source.ParsedContent;
import com.recallio.content.domain.source.ParsedContent.ParsedCard;
import com.recallio.content.domain.source.ParsedContent.ParsedTheme;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Compare le document d'un sujet à ce que le catalogue contient pour ce sujet, et décide des changements. Code pur, sans base de données : toute la logique
 * d'idempotence et de versionnement se teste unitairement.
 *
 * <ul>
 *   <li>carte absente du catalogue : ajoutée (version 1) ;
 *   <li>carte présente dont le texte a changé : nouvelle version, même identifiant ;
 *   <li>carte du sujet absente du document : retirée (jamais supprimée, la progression des utilisateurs y est
 *       rattachée). Les cartes des autres sujets ne font pas partie de la comparaison, donc ne sont jamais retirées ;
 *   <li>carte retirée qui réapparaît : réactivée.
 * </ul>
 */
public final class ImportPlanner {

    private static final int EXCERPT_LENGTH = 80;

    /**
     * @param catalog l'état en base du sujet décrit par {@code source} ({@link CatalogStore#snapshot}) : seules ses
     *     cartes peuvent être retirées
     */
    public ImportPlan plan(ParsedContent source, CatalogSnapshot catalog) {
        var plan = new PlanBuilder();
        Subject subject = source.subject();
        UUID subjectId = CardIdentity.subjectId(subject.slug());
        plan.subjectUpsert = subject.equals(catalog.subject()) ? null : new SubjectUpsert(subjectId, subject);
        Set<UUID> seenCardIds = new HashSet<>();
        for (ParsedTheme theme : source.themes()) {
            UUID themeId = CardIdentity.themeId(subject.slug(), theme.key());
            planTheme(theme, themeId, subjectId, CardIdentity.themeSlug(subject.slug(), theme.title()), catalog, plan);
            int position = 0;
            for (ParsedCard card : theme.cards()) {
                UUID cardId = CardIdentity.cardId(
                        subject.slug(), theme.key(), card.content().prompt());
                if (!seenCardIds.add(cardId)) {
                    plan.duplicates.add(new Duplicate(theme.key(), card.sourceLine(), excerpt(card)));
                    continue;
                }
                planCard(cardId, themeId, position++, card.content(), catalog, plan);
            }
        }
        catalog.cards().values().stream()
                .filter(existing -> !existing.retired() && !seenCardIds.contains(existing.id()))
                .forEach(existing -> plan.retired.add(existing.id()));
        return plan.build(source);
    }

    private static void planTheme(
            ParsedTheme theme, UUID themeId, UUID subjectId, String slug, CatalogSnapshot catalog, PlanBuilder plan) {
        var upsert = new ThemeUpsert(themeId, subjectId, slug, theme.title(), theme.position());
        ExistingTheme existing = catalog.themes().get(themeId);
        if (existing == null) {
            plan.themes.add(upsert);
            plan.themesCreated++;
        } else if (!existing.equals(new ExistingTheme(themeId, slug, theme.title(), theme.position()))) {
            plan.themes.add(upsert);
            plan.themesUpdated++;
        }
    }

    private static void planCard(
            UUID cardId, UUID themeId, int position, CardContent content, CatalogSnapshot catalog, PlanBuilder plan) {
        String hash = CardIdentity.contentHash(content);
        ExistingCard existing = catalog.cards().get(cardId);
        if (existing == null) {
            plan.newCards.add(new NewCard(cardId, themeId, position, content, hash));
            return;
        }
        if (existing.retired()) {
            plan.reactivated.add(cardId);
        }
        if (existing.contentHash().equals(hash)) {
            plan.unchanged++;
        } else {
            plan.newVersions.add(new NewVersion(cardId, existing.currentVersion() + 1, content, hash));
        }
        boolean moved = !existing.themeId().equals(themeId)
                || existing.type() != content.type()
                || existing.position() != position;
        if (moved) {
            plan.placements.add(new CardPlacement(cardId, themeId, content.type(), position));
        }
    }

    private static String excerpt(ParsedCard card) {
        String prompt = card.content().prompt().strip();
        return prompt.length() <= EXCERPT_LENGTH ? prompt : prompt.substring(0, EXCERPT_LENGTH) + "…";
    }

    /** Accumulateur mutable, limité à la durée d'un appel à {@link #plan}. */
    private static final class PlanBuilder {
        private final List<ThemeUpsert> themes = new ArrayList<>();
        private final List<NewCard> newCards = new ArrayList<>();
        private final List<NewVersion> newVersions = new ArrayList<>();
        private final List<CardPlacement> placements = new ArrayList<>();
        private final List<UUID> retired = new ArrayList<>();
        private final List<UUID> reactivated = new ArrayList<>();
        private final List<Duplicate> duplicates = new ArrayList<>();
        private @Nullable SubjectUpsert subjectUpsert;
        private int themesCreated;
        private int themesUpdated;
        private int unchanged;

        private ImportPlan build(ParsedContent source) {
            var report = new ImportReport(
                    source.subject().slug(),
                    themesCreated,
                    themesUpdated,
                    newCards.size(),
                    newVersions.size(),
                    unchanged,
                    retired.size(),
                    reactivated.size(),
                    duplicates,
                    source.issues());
            return new ImportPlan(
                    subjectUpsert, themes, newCards, newVersions, placements, retired, reactivated, report);
        }
    }
}
