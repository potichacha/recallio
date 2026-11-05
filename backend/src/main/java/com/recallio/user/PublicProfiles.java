package com.recallio.user;

import java.util.Optional;

/** API publique du module user (ADR 0001) : retrouver un profil public par son adresse (ADR 0043). */
public interface PublicProfiles {

    /**
     * @param handle l'adresse du profil, telle qu'écrite dans l'URL
     * @return vide si personne n'a cette adresse, ou si son profil est privé : les deux cas se confondent
     */
    Optional<PublicUser> visible(String handle);

    /**
     * Un utilisateur dont le profil est public.
     *
     * @param user pour lire sa progression ; son identifiant et son fuseau ne sont pas publiés
     */
    record PublicUser(UserRef user, String handle, String displayName) {}
}
