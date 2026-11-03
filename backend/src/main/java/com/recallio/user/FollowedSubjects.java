package com.recallio.user;

import java.util.List;
import java.util.Map;

/** API publique du module user (ADR 0001) : les sujets que suit un utilisateur, qui alimentent ses révisions. */
public interface FollowedSubjects {

    /** Les slugs des sujets publiés que suit l'utilisateur, dans l'ordre du catalogue ; vide s'il n'en a choisi aucun. */
    List<String> of(UserRef user);

    /** Le parcours de chaque sujet suivi (ADR 0032), par slug ; un sujet absent suit {@link LearningTrack#DEFAULT}. */
    Map<String, LearningTrack> tracksOf(UserRef user);
}
