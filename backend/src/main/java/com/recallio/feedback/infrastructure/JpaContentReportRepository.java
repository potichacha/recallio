package com.recallio.feedback.infrastructure;

import com.recallio.feedback.domain.ContentReport;
import com.recallio.feedback.domain.ContentReportRepository;
import com.recallio.feedback.domain.ReportResolution;
import com.recallio.feedback.domain.ReportTarget;
import com.recallio.feedback.domain.ReportTarget.CardTarget;
import com.recallio.feedback.domain.ReportTarget.TourPageTarget;
import com.recallio.platform.domain.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaContentReportRepository implements ContentReportRepository {

    private final EntityManager entityManager;

    public JpaContentReportRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void add(ContentReport report) {
        var entity = new ContentReportEntity();
        entity.id = report.id();
        entity.userId = report.userId();
        entity.subjectSlug = report.subjectSlug();
        switch (report.target()) {
            case CardTarget(UUID cardId) -> entity.cardId = cardId;
            case TourPageTarget(String _, String notionSlug, int page) -> {
                entity.notionSlug = notionSlug;
                entity.page = page;
            }
        }
        entity.reason = report.reason();
        entity.comment = report.comment();
        entity.createdAt = report.createdAt();
        entityManager.persist(entity);
    }

    @Override
    public boolean hasOpenReport(UUID userId, ReportTarget target) {
        var query = switch (target) {
            case CardTarget(UUID cardId) ->
                entityManager.createQuery("""
                                        select count(r) from ContentReportEntity r
                                        where r.userId = :userId and r.resolution is null and r.cardId = :cardId
                                        """, Long.class).setParameter("cardId", cardId);
            case TourPageTarget(String subjectSlug, String notionSlug, int page) ->
                entityManager
                        .createQuery("""
                                        select count(r) from ContentReportEntity r
                                        where r.userId = :userId and r.resolution is null
                                          and r.subjectSlug = :subject and r.notionSlug = :notion and r.page = :page
                                        """, Long.class)
                        .setParameter("subject", subjectSlug)
                        .setParameter("notion", notionSlug)
                        .setParameter("page", page);
        };
        return query.setParameter("userId", userId).getSingleResult() > 0;
    }

    @Override
    public long openReportsBy(UUID userId) {
        return entityManager
                .createQuery(
                        "select count(r) from ContentReportEntity r where r.userId = :userId and r.resolution is null",
                        Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    @Override
    public Optional<ContentReport> find(UUID id) {
        return Optional.ofNullable(entityManager.find(ContentReportEntity.class, id))
                .map(JpaContentReportRepository::toDomain);
    }

    @Override
    public Page<ContentReport> list(boolean open, int page, int size) {
        String where = open ? "r.resolution is null" : "r.resolution is not null";
        String order = open ? "r.createdAt, r.id" : "r.resolvedAt desc, r.id";
        List<ContentReport> items = entityManager
                .createQuery(
                        "select r from ContentReportEntity r where " + where + " order by " + order,
                        ContentReportEntity.class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultStream()
                .map(JpaContentReportRepository::toDomain)
                .toList();
        long total = entityManager
                .createQuery("select count(r) from ContentReportEntity r where " + where, Long.class)
                .getSingleResult();
        return new Page<>(items, page, size, total);
    }

    @Override
    public boolean resolve(UUID id, ReportResolution resolution, Instant resolvedAt) {
        return entityManager
                        .createQuery("""
                                update ContentReportEntity r set r.resolution = :resolution, r.resolvedAt = :at
                                where r.id = :id and r.resolution is null
                                """)
                        .setParameter("resolution", resolution)
                        .setParameter("at", resolvedAt)
                        .setParameter("id", id)
                        .executeUpdate()
                > 0;
    }

    private static ContentReport toDomain(ContentReportEntity entity) {
        ReportTarget target = entity.cardId != null
                ? new CardTarget(entity.cardId)
                : new TourPageTarget(entity.subjectSlug, entity.notionSlug, entity.page);
        return new ContentReport(
                entity.id,
                entity.userId,
                entity.subjectSlug,
                target,
                entity.reason,
                entity.comment,
                entity.createdAt,
                entity.resolution,
                entity.resolvedAt);
    }
}
