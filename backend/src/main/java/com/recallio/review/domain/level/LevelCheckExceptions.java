package com.recallio.review.domain.level;

import com.recallio.platform.domain.DomainException;
import com.recallio.platform.domain.ResourceNotFoundException;

/** Les refus du test de niveau (ADR 0068), chacun avec son code d'erreur stable. */
public final class LevelCheckExceptions {

    private LevelCheckExceptions() {}

    /** Le sujet n'a aucune notion débutante avec une question que le serveur corrige : 422. */
    public static final class LevelCheckUnavailableException extends DomainException {

        public static final String CODE = "level-check.unavailable";

        public LevelCheckUnavailableException(String subjectSlug) {
            super(
                    CODE,
                    "Le sujet « %s » n'a pas encore de test de niveau : commence par son Tour ou ses révisions."
                            .formatted(subjectSlug));
        }
    }

    /** Le test du sujet n'a pas ce domaine : il n'existe pas, ou n'a pas de notion débutante avec des questions. */
    public static final class LevelCheckDomainNotFoundException extends ResourceNotFoundException {

        public static final String CODE = "level-check.domain-not-found";

        public LevelCheckDomainNotFoundException(String subjectSlug, String domain) {
            super(CODE, "Le test de niveau de « %s » n'a pas de domaine « %s ».".formatted(subjectSlug, domain));
        }
    }

    /**
     * Les réponses ne sont pas celles d'un tirage du domaine : pas autant que ses questions, une carte d'ailleurs, ou deux
     * cartes d'une même notion. 422.
     */
    public static final class InvalidLevelCheckAnswersException extends DomainException {

        public static final String CODE = "level-check.invalid-answers";

        private static final String DETAIL = "Le domaine « %s » attend une réponse à chacune de ses %d question(s),"
                + " chacune sur une notion débutante différente : recharge le test.";

        public InvalidLevelCheckAnswersException(String domainTitle, int questionCount) {
            super(CODE, DETAIL.formatted(domainTitle, questionCount));
        }
    }
}
