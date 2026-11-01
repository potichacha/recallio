package com.recallio.user;

import java.util.List;

/** API publique du module user (ADR 0001) : les sujets que suit un utilisateur, qui alimentent ses révisions. */
public interface FollowedSubjects {

    /** Les slugs des sujets publiés que suit l'utilisateur, dans l'ordre du catalogue ; vide s'il n'en a choisi aucun. */
    List<String> of(UserRef user);
}
