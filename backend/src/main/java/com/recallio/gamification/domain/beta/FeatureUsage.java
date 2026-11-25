package com.recallio.gamification.domain.beta;

import com.recallio.platform.domain.UsageFeature;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * L'usage de chaque fonctionnalité sur la fenêtre (ADR 0076), rapporté aux comptes actifs.
 *
 * <p>Un compte est actif s'il a utilisé au moins une fonctionnalité datée sur la fenêtre : réviser, mais aussi valider
 * une notion du Tour ou jouer un duel. Qui apprend commence par le Tour, sans réviser : il est actif.
 *
 * @param activeAccounts les comptes actifs sur la fenêtre
 * @param features chaque fonctionnalité, dans l'ordre de {@link UsageFeature}
 */
public record FeatureUsage(int activeAccounts, List<FeatureAdoption> features) {

    public FeatureUsage {
        features = List.copyOf(features);
    }

    /**
     * @param users les comptes de chaque fonctionnalité : sur la fenêtre si elle est datée, à ce jour sinon ; une
     *     fonctionnalité absente n'a aucun compte
     */
    public static FeatureUsage of(Map<UsageFeature, Set<UUID>> users) {
        Set<UUID> active = new HashSet<>();
        for (UsageFeature feature : UsageFeature.values()) {
            if (feature.isDated()) {
                active.addAll(users.getOrDefault(feature, Set.of()));
            }
        }
        return new FeatureUsage(
                active.size(),
                Arrays.stream(UsageFeature.values())
                        .map(feature -> adoption(feature, users.getOrDefault(feature, Set.of()), active))
                        .toList());
    }

    /** Un compte qui a un rappel ou un groupe mais n'a rien fait sur la fenêtre n'est pas compté : il n'est pas actif. */
    private static FeatureAdoption adoption(UsageFeature feature, Set<UUID> users, Set<UUID> active) {
        long accounts = users.stream().filter(active::contains).count();
        return new FeatureAdoption(feature, Math.toIntExact(accounts), BetaReport.percent(accounts, active.size()));
    }

    /**
     * @param accounts les comptes actifs qui ont utilisé la fonctionnalité sur la fenêtre ; pour une fonctionnalité qui
     *     n'est pas datée, ceux qui l'ont à ce jour
     * @param percent leur part des comptes actifs, en pourcentage arrondi vers le bas ; {@code null} quand aucun compte
     *     n'est actif
     */
    public record FeatureAdoption(
            UsageFeature feature, int accounts, @Nullable Integer percent) {}
}
