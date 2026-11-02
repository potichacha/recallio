package com.recallio.content.infrastructure.persistence;

import com.recallio.content.CardType;
import com.recallio.content.domain.CardContent;
import com.recallio.content.domain.importing.CatalogSnapshot;
import com.recallio.content.domain.importing.CatalogSnapshot.ExistingCard;
import com.recallio.content.domain.importing.CatalogSnapshot.ExistingNotion;
import com.recallio.content.domain.importing.CatalogSnapshot.ExistingTheme;
import com.recallio.content.domain.importing.CatalogStore;
import com.recallio.content.domain.importing.ImportPlan;
import com.recallio.content.domain.importing.ImportPlan.CardPlacement;
import com.recallio.content.domain.importing.ImportPlan.NewVersion;
import com.recallio.content.domain.importing.ImportPlan.NotionUpsert;
import com.recallio.content.domain.importing.ImportPlan.SubjectUpsert;
import com.recallio.content.domain.importing.ImportPlan.ThemeUpsert;
import com.recallio.platform.domain.IdGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Persistance de l'import. Doit être appelée dans une transaction (portée par le service applicatif). */
@ApplicationScoped
public class JpaCatalogStore implements CatalogStore {

    private final EntityManager entityManager;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public JpaCatalogStore(EntityManager entityManager, IdGenerator idGenerator, Clock clock) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Override
    public CatalogSnapshot snapshot(UUID subjectId) {
        SubjectEntity subject = entityManager.find(SubjectEntity.class, subjectId);
        Map<UUID, ExistingTheme> themes = entityManager
                .createQuery("select t from ThemeEntity t where t.subjectId = :subjectId", ThemeEntity.class)
                .setParameter("subjectId", subjectId)
                .getResultStream()
                .map(theme ->
                        new ExistingTheme(theme.id, theme.slug, theme.title, theme.position, theme.retiredAt != null))
                .collect(Collectors.toMap(ExistingTheme::id, Function.identity()));
        Map<UUID, ExistingNotion> notions = entityManager
                .createQuery("select n from NotionEntity n where n.subjectId = :subjectId", NotionEntity.class)
                .setParameter("subjectId", subjectId)
                .getResultStream()
                .map(notion -> new ExistingNotion(notion.id, notion.toParsed(), notion.retiredAt != null))
                .collect(Collectors.toMap(ExistingNotion::id, Function.identity()));
        Map<UUID, ExistingCard> cards = entityManager
                .createQuery("""
                        select c.id, c.themeId, c.notionId, c.type, c.position, c.currentVersion, v.contentHash, c.retiredAt
                        from CardEntity c
                        join CardVersionEntity v on v.cardId = c.id and v.version = c.currentVersion
                        join ThemeEntity t on t.id = c.themeId
                        where t.subjectId = :subjectId
                        """, Object[].class)
                .setParameter("subjectId", subjectId)
                .getResultStream()
                .map(JpaCatalogStore::toExistingCard)
                .collect(Collectors.toMap(ExistingCard::id, Function.identity()));
        return new CatalogSnapshot(subject == null ? null : subject.toDomain(), themes, notions, cards);
    }

    @Override
    public void apply(ImportPlan plan) {
        Instant now = clock.instant();
        // Sujet, thèmes et notions, puis cartes, puis versions : les insertions restent groupées par table (batch JDBC)
        // et les clés étrangères sont satisfaites.
        SubjectUpsert subject = plan.subjectUpsert();
        if (subject != null) {
            upsertSubject(subject, now);
        }
        retireThemes(plan, now);
        plan.themes().forEach(theme -> upsertTheme(theme, now));
        plan.retiredNotionIds().forEach(id -> notion(id).retiredAt = now);
        plan.notions().forEach(notion -> upsertNotion(notion, now));
        plan.newCards()
                .forEach(card -> entityManager.persist(new CardEntity(
                        card.id(),
                        card.themeId(),
                        card.notionId(),
                        card.content().type(),
                        card.position(),
                        now)));
        plan.newCards().forEach(card -> persistVersion(card.id(), 1, card.content(), card.contentHash(), now));
        plan.newVersions().forEach(version -> addVersion(version, now));
        plan.placements().forEach(this::place);
        plan.retiredCardIds().forEach(id -> card(id).retiredAt = now);
        plan.reactivatedCardIds().forEach(id -> card(id).retiredAt = null);
    }

    private void upsertSubject(SubjectUpsert upsert, Instant now) {
        SubjectEntity existing = entityManager.find(SubjectEntity.class, upsert.id());
        if (existing == null) {
            entityManager.persist(new SubjectEntity(upsert.id(), upsert.subject(), now));
            // Le sujet doit exister en base avant les thèmes qui le référencent (insertions en batch).
            entityManager.flush();
        } else {
            existing.update(upsert.subject(), now);
        }
    }

    /** Les thèmes retirés libèrent leur slug avant qu'un nouveau thème ne le reprenne (index unique partiel, V14). */
    private void retireThemes(ImportPlan plan, Instant now) {
        if (plan.retiredThemeIds().isEmpty()) {
            return;
        }
        plan.retiredThemeIds().forEach(id -> theme(id).retiredAt = now);
        entityManager.flush();
    }

    private void upsertNotion(NotionUpsert upsert, Instant now) {
        NotionEntity existing = entityManager.find(NotionEntity.class, upsert.id());
        if (existing == null) {
            entityManager.persist(new NotionEntity(upsert.id(), upsert.subjectId(), upsert.notion(), now));
        } else {
            existing.update(upsert.notion(), now);
        }
    }

    private void upsertTheme(ThemeUpsert theme, Instant now) {
        ThemeEntity existing = entityManager.find(ThemeEntity.class, theme.id());
        if (existing == null) {
            entityManager.persist(
                    new ThemeEntity(theme.id(), theme.subjectId(), theme.slug(), theme.title(), theme.position(), now));
        } else {
            existing.update(theme.slug(), theme.title(), theme.position(), now);
        }
    }

    private void addVersion(NewVersion version, Instant now) {
        persistVersion(version.cardId(), version.version(), version.content(), version.contentHash(), now);
        card(version.cardId()).currentVersion = version.version();
    }

    private void persistVersion(UUID cardId, int version, CardContent content, String hash, Instant now) {
        entityManager.persist(new CardVersionEntity(idGenerator.newId(), cardId, version, content, hash, now));
    }

    private void place(CardPlacement placement) {
        CardEntity card = card(placement.cardId());
        card.themeId = placement.themeId();
        card.notionId = placement.notionId();
        card.type = placement.type();
        card.position = placement.position();
    }

    private CardEntity card(UUID id) {
        CardEntity card = entityManager.find(CardEntity.class, id);
        if (card == null) {
            throw new IllegalStateException("Carte introuvable pendant l'import : " + id);
        }
        return card;
    }

    private ThemeEntity theme(UUID id) {
        ThemeEntity theme = entityManager.find(ThemeEntity.class, id);
        if (theme == null) {
            throw new IllegalStateException("Thème introuvable pendant l'import : " + id);
        }
        return theme;
    }

    private NotionEntity notion(UUID id) {
        NotionEntity notion = entityManager.find(NotionEntity.class, id);
        if (notion == null) {
            throw new IllegalStateException("Notion introuvable pendant l'import : " + id);
        }
        return notion;
    }

    private static ExistingCard toExistingCard(Object[] row) {
        return new ExistingCard(
                (UUID) row[0],
                (UUID) row[1],
                (UUID) row[2],
                (CardType) row[3],
                (Integer) row[4],
                (Integer) row[5],
                (String) row[6],
                row[7] != null);
    }
}
