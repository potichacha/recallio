package com.recallio.content.application;

import com.recallio.content.domain.catalog.ContentCatalog;
import com.recallio.content.domain.catalog.ContentCatalog.SubjectSummary;
import com.recallio.content.domain.catalog.ContentCatalog.TourEntry;
import com.recallio.content.domain.catalog.ContentCatalog.Visibility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Le plan du site (ADR 0065) : les pages publiques que les moteurs de recherche ont intérêt à trouver. Seulement celles
 * des sujets publiés, jamais d'un brouillon, même annoncé ; ni les thèmes, ni les profils publics, ni l'essai.
 *
 * <p>Ces chemins suivent les routes de la PWA ({@code frontend/src/routes.tsx}) : une adresse qui change là-bas change
 * ici.
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class SitemapService {

    /** L'accueil, les pages Cours et Tour, le catalogue, puis les deux pages légales (ADR 0074). */
    static final List<String> FIXED_PATHS =
            List.of("/", "/learn", "/tours", "/subjects", "/mentions-legales", "/confidentialite");

    private final ContentCatalog catalog;

    public SitemapService(ContentCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Les chemins des pages, sans l'origine : les pages fixes, puis pour chaque sujet publié, dans l'ordre du catalogue,
     * sa page, ses leçons, et son Tour s'il en a un.
     */
    public List<String> paths() {
        List<String> paths = new ArrayList<>(FIXED_PATHS);
        for (SubjectSummary subject : catalog.subjects(Visibility.PUBLISHED_ONLY)) {
            String subjectPath = "/subjects/" + segment(subject.slug());
            paths.add(subjectPath);
            for (String notion : catalog.lessonNotions(subject.id())) {
                paths.add(subjectPath + "/course/" + segment(notion));
            }
            List<TourEntry> tour = catalog.tour(subject.id());
            if (!tour.isEmpty()) {
                paths.add(subjectPath + "/tour");
                for (TourEntry entry : tour) {
                    paths.add(subjectPath + "/tour/" + segment(entry.slug()));
                }
            }
        }
        return List.copyOf(paths);
    }

    /**
     * Un segment de chemin, encodé comme le fait la PWA ({@code encodeURIComponent}) : l'identifiant d'une notion n'a
     * pas de forme imposée.
     */
    private static String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
