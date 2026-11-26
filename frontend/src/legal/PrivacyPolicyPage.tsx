import { Link } from 'react-router'
import { legalConfig } from './legalConfig'
import { ContactEmail, LegalList, LegalPage, LegalSection, LegalTable } from './LegalText'
import { TEXT_LINK } from './textLink'

/** À changer à chaque modification du texte : c'est la version que le lecteur a lue. */
const UPDATED_ON = '26 novembre 2026'

const WHILE_ACCOUNT = 'Tant que ton compte existe'
const DATA_HEAD = ['Quoi', 'Pourquoi', 'Combien de temps']

/**
 * La politique de confidentialité (ADR 0074). Elle dit ce que le code fait, ni plus ni moins : chaque ligne renvoie à
 * une table, à un stockage du navigateur ou à un réglage du serveur, que l'ADR liste avec sa preuve. Une donnée de
 * plus, un stockage de plus ou un sous-traitant de plus se reporte ici, avec la date en tête.
 */
export function PrivacyPolicyPage() {
  const { publisher, contactEmail } = legalConfig()
  return (
    <LegalPage title="Politique de confidentialité" updatedOn={UPDATED_ON}>
      <LegalSection title="En bref">
        <LegalList>
          <li>Aucune publicité, aucun traceur, aucun outil de mesure d’audience tiers.</li>
          <li>
            Recallio garde ton compte et ta progression, pour te faire réviser. Rien n’est vendu ni
            transmis à des fins publicitaires.
          </li>
          <li>
            Depuis ton profil, tu télécharges tout ce que Recallio garde sur toi, et tu supprimes
            ton compte, à tout moment.
          </li>
        </LegalList>
      </LegalSection>

      <LegalSection title="Qui est responsable de tes données">
        <p>
          {publisher ? (
            <>
              <strong className="text-ink">{publisher}</strong>, l’éditeur de Recallio, un
              particulier
            </>
          ) : (
            <>L’éditeur de Recallio, un particulier</>
          )}{' '}
          (voir les{' '}
          <Link to="/mentions-legales" className={TEXT_LINK}>
            mentions légales
          </Link>
          ). Pour toute question ou toute demande sur tes données :{' '}
          <ContactEmail email={contactEmail} />.
        </p>
      </LegalSection>

      <LegalSection title="Ce que Recallio garde">
        <LegalTable
          caption="Sans compte"
          head={DATA_HEAD}
          rows={[
            [
              'Ta visite de la page d’accueil',
              'Une ligne « une visite », avec sa date et son heure, sans identifiant, sans adresse IP ni cookie : elle sert à compter les visiteurs, pas à les reconnaître.',
              'Sans limite : la ligne ne désigne personne',
            ],
            [
              'L’essai sans compte',
              'Tes réponses sont corrigées par le serveur et ne sont pas enregistrées. Seule une ligne « un essai commencé » est gardée, sans identifiant.',
              'Sans limite : la ligne ne désigne personne',
            ],
            [
              'Le journal du serveur web',
              'Pour comprendre une panne : l’heure, la page ou l’appel demandé, le code de réponse, la taille et la durée. Ni adresse IP, ni navigateur, ni page d’origine, ni paramètres de l’adresse ; le jeton d’une invitation et l’adresse d’un profil public y sont masqués.',
              'Jusqu’à ce que les fichiers tournent (50 Mo par service)',
            ],
            [
              'Ton adresse IP',
              'Pour limiter le nombre de requêtes sans compte venant d’une même adresse : en mémoire, sans l’écrire. Elle peut s’écrire, avec la requête, dans le journal d’erreurs du serveur web lors d’une erreur technique rare (un service injoignable, une requête mal formée).',
              'En mémoire : le temps du comptage. Dans le journal d’erreurs : jusqu’à ce que les fichiers tournent',
            ],
          ]}
        />
        <LegalTable
          caption="Avec un compte"
          head={DATA_HEAD}
          rows={[
            [
              'Ton identité',
              'Pour te connecter, vérifier ton adresse et te renvoyer un mot de passe oublié : ton nom d’utilisateur, ton adresse e-mail, ton prénom, ton nom et ton mot de passe, haché. Ils sont dans Keycloak, le service de connexion, installé sur le serveur de Recallio.',
              WHILE_ACCOUNT,
            ],
            [
              'Tes sessions de connexion',
              'Pour te garder connecté : Keycloak note le début de la session et l’adresse IP d’où tu t’es connecté.',
              'Le temps de la session : 30 minutes sans activité, 10 heures au plus ; avec « Se souvenir de moi », 30 jours sans visite, 90 jours au plus',
            ],
            [
              'Tes échecs de connexion',
              'Pour bloquer les essais de mots de passe : Keycloak compte les échecs d’un compte et garde l’adresse IP du dernier.',
              'Le temps de cette protection',
            ],
            [
              'Ton profil',
              'Pour faire fonctionner ton compte : ton nom affiché (repris de ton identité à la première connexion, modifiable), ton fuseau horaire (celui de ton navigateur, modifiable : il sert à savoir quand ta journée commence), tes sujets suivis et ton parcours.',
              WHILE_ACCOUNT,
            ],
            [
              'Ta progression',
              'Pour te reposer chaque carte au bon moment et te montrer où tu en es : l’état de ta mémoire pour chaque carte, l’historique de tes réponses (la carte, le résultat, la date, le temps mis), tes examens blancs, ton avancement dans le Tour, tes tests de niveau, les dates de tes entretiens, tes réponses aux défis du jour. En révision intensive, tes réponses sont corrigées sans être enregistrées : seul un compteur est gardé, le nombre de réponses de chaque jour et combien étaient justes. L’éditeur en tire aussi des chiffres d’ensemble (combien de comptes révisent, quelles cartes sont le plus ratées), sans fiche par personne, pour améliorer le contenu.',
              WHILE_ACCOUNT,
            ],
            [
              'Ton XP et ta série',
              'Pour le jeu : l’XP gagnée, ton activité de chaque jour, tes quêtes et tes succès.',
              WHILE_ACCOUNT,
            ],
            [
              'Tes groupes',
              'Pour réviser à plusieurs : les groupes dont tu es membre, les défis que tu lances, tes réponses, tes résultats aux parties en direct. Les autres membres voient ton nom affiché et ta place au classement.',
              'Tant que ton compte existe ; ce qui a été joué à plusieurs reste ensuite, sans ton nom (voir « Supprimer ton compte »)',
            ],
            [
              'Le classé',
              'Pour les duels : ta cote, ton rang, tes insignes, tes duels et tes réponses. Ton adversaire voit ton nom affiché.',
              'Tant que ton compte existe ; les duels joués restent ensuite, sans ton nom',
            ],
            [
              'Ton profil public',
              'Seulement si tu l’actives : l’adresse que tu choisis. Elle montre à tous ton nom affiché, ta série, tes succès et, par sujet, ton niveau, ta maîtrise et tes réussites. Jamais ton identifiant, ton fuseau horaire, tes groupes ni ton historique carte par carte.',
              'Jusqu’à ce que tu le rendes privé ou que tu supprimes ton compte',
            ],
            [
              'Tes rappels',
              'Seulement si tu les acceptes, pour chaque appareil : l’adresse technique que ton navigateur fournit pour le joindre, deux clés de chiffrement, l’heure choisie et la date du dernier rappel.',
              'Jusqu’à ce que tu les coupes, que tu supprimes ton compte, ou que ton navigateur annonce que l’appareil est désabonné',
            ],
            [
              'Tes signalements',
              'Pour corriger le contenu : la carte ou la page signalée, la raison, tes précisions et la date. Ton nom n’est montré à personne.',
              'Sans limite, même après la suppression du compte : ils ne mènent alors plus à toi',
            ],
            [
              'Le code que tu écris',
              'Pour l’exécuter : le code d’un exercice ou d’une page du Tour part sur un second serveur de Recallio, sans rien qui dise de qui il vient, tourne dans un conteneur jetable, et n’est pas gardé.',
              'Le temps de l’exécution',
            ],
          ]}
        />
        <LegalTable
          caption="Sur ton appareil"
          head={DATA_HEAD}
          rows={[
            [
              'Le stockage local du navigateur',
              'Pour tes réglages sur cet appareil : le thème, les sons et les vibrations, le sujet et la durée de ta dernière session, le nombre d’ouvertures de l’application (pour ne proposer de l’installer qu’à la deuxième), la date où tu as remis les rappels à plus tard, et, pendant une connexion, son état technique.',
              'Jusqu’à ce que tu effaces les données du site',
            ],
            [
              'Le stockage de session',
              'Pour rester connecté dans l’onglet : tes jetons de connexion ; et deux repères techniques (visite déjà comptée, page déjà rechargée après une mise à jour).',
              'Jusqu’à la fermeture de l’onglet',
            ],
            [
              'Le cache de l’application',
              'Pour s’ouvrir vite et hors ligne : les fichiers de l’application (code, styles, polices, icônes). Aucune de tes données.',
              'Jusqu’à ce que tu effaces les données du site',
            ],
            [
              'Les cookies',
              'L’application n’en pose aucun. Keycloak, sur son adresse à lui, pose ceux de ta session de connexion, strictement nécessaires.',
              'Le temps de la session de connexion (ci-dessus)',
            ],
          ]}
        />
        <p>
          Recallio ne garde ni ton adresse e-mail ni ton mot de passe ailleurs que dans Keycloak, et
          ne supprime pas un compte parce qu’il est inactif : c’est toi qui décides.
        </p>
      </LegalSection>

      <LegalSection title="Ce qui autorise ces traitements">
        <LegalList>
          <li>
            <strong className="text-ink">Le service que tu demandes</strong> : ton compte, ta
            progression, l’XP, les groupes, le classé, et le profil public si tu l’actives.
          </li>
          <li>
            <strong className="text-ink">L’intérêt légitime de l’éditeur</strong> à protéger et à
            améliorer le service : la limite de requêtes, le blocage des essais de mots de passe,
            les journaux, les sauvegardes, les comptes de visites et d’essais sans identifiant, les
            signalements d’erreurs, et les chiffres d’ensemble tirés de l’activité des comptes
            (combien révisent, quelles cartes sont le plus ratées), sans fiche par personne.
          </li>
          <li>
            <strong className="text-ink">Ton consentement</strong> : les rappels par notification.
            Tu le retires dans ton profil, ou dans les réglages de ton navigateur.
          </li>
        </LegalList>
      </LegalSection>

      <LegalSection title="Qui d’autre traite tes données">
        <p>Recallio fait appel à ces prestataires, et à eux seuls :</p>
        <LegalTable
          caption="Les prestataires"
          head={['Qui', 'Pour quoi', 'Ce qu’il reçoit']}
          rows={[
            [
              'OVHcloud (OVH SAS, France)',
              'Héberge les deux serveurs de Recallio.',
              'Tout ce que ces serveurs gardent.',
            ],
            [
              'Backblaze (Backblaze, Inc., États-Unis ; stockage dans l’Union européenne)',
              'Garde les sauvegardes de chaque nuit.',
              'Des fichiers chiffrés avant l’envoi, avec une clé qu’il n’a pas : il ne peut pas les lire.',
            ],
            [
              'Brevo (Sendinblue SAS, France)',
              'Envoie les e-mails de vérification de ton adresse et de mot de passe oublié.',
              'Ton adresse e-mail et le contenu de ces e-mails.',
            ],
            [
              'Sentry (Functional Software, Inc., États-Unis ; région Union européenne), quand le suivi des erreurs est activé',
              'Reçoit les erreurs techniques, pour les corriger.',
              'Le message de l’erreur, la page où elle s’est produite et le type de navigateur. Ni ton identité, ni tes cookies, ni les paramètres de l’adresse ; le jeton d’une invitation et l’adresse d’un profil public sont masqués. Sentry voit l’adresse IP d’où part l’envoi : Recallio lui demande de ne pas la garder.',
            ],
            [
              'Le service de notification de ton navigateur (Google, Mozilla, Apple ou Microsoft), si tu acceptes les rappels',
              'Achemine le rappel jusqu’à ton appareil.',
              'Un message chiffré pour ton appareil, qu’il ne peut pas lire.',
            ],
          ]}
        />
        <p>
          Backblaze, Sentry et les services de notification sont des sociétés américaines. Les
          sauvegardes et les erreurs sont stockées dans l’Union européenne.
        </p>
      </LegalSection>

      <LegalSection title="Les sauvegardes">
        <LegalList>
          <li>
            Chaque nuit, les bases de données sont sauvegardées, chiffrées sur le serveur, puis
            envoyées chez Backblaze. La clé qui les déchiffre n’est sur aucun serveur.
          </li>
          <li>
            Une sauvegarde est gardée 30 jours, et supprimée au plus tard 32 jours après son envoi.
          </li>
          <li>
            Un compte supprimé disparaît tout de suite de l’application, mais reste dans les
            sauvegardes jusqu’à leur suppression, 32 jours au plus. Elles ne sont lues que pour
            restaurer le service.
          </li>
          <li>
            Si une sauvegarde doit être restaurée, les suppressions de compte faites depuis sont
            refaites.
          </li>
        </LegalList>
      </LegalSection>

      <LegalSection title="Cookies et traceurs">
        <p>
          Recallio n’utilise ni cookie publicitaire, ni traceur, ni outil de mesure d’audience
          tiers. Ce qui est posé sur ton appareil (voir « Sur ton appareil ») ne sert qu’à faire
          fonctionner l’application pour toi : rester connecté, retrouver tes réglages, l’ouvrir
          hors ligne. Rien n’y sert à te suivre d’un site à l’autre ni d’une visite à l’autre. C’est
          pourquoi aucun bandeau ne te demande d’accepter des cookies.
        </p>
      </LegalSection>

      <LegalSection title="Tes droits">
        <LegalList>
          <li>
            <strong className="text-ink">Accéder à tes données et les emporter</strong> : «
            Télécharger mes données », dans ton profil, section « Mes données ». Le fichier contient
            tout ce que l’application garde sur toi. Ton identité chez Keycloak se lit et se modifie
            sur sa page de compte ; écris-nous si tu en veux une copie.
          </li>
          <li>
            <strong className="text-ink">Les corriger</strong> : ton nom affiché et ton fuseau
            horaire, dans ton profil.
          </li>
          <li>
            <strong className="text-ink">Supprimer ton compte</strong> : « Supprimer mon compte »,
            au même endroit. Ta progression, ton XP, ta série, ton rang, ton profil public et tes
            rappels sont effacés tout de suite. Ce qui a été joué à plusieurs (duels, défis de
            groupe, parties en direct) reste, sous le nom « Compte supprimé », ainsi que tes
            signalements : plus rien n’y mène à toi. Recallio t’envoie ensuite chez Keycloak, qui te
            fait confirmer la suppression de ton identité ; si tu y renonces, elle reste, et il faut
            la supprimer depuis ta page de compte ou nous écrire.
          </li>
          <li>
            <strong className="text-ink">T’opposer à un traitement, ou le faire limiter</strong> :
            écris à <ContactEmail email={contactEmail} />.
          </li>
        </LegalList>
        <p>
          Tu reçois une réponse dans un délai d’un mois. Si elle ne te convient pas, tu peux saisir
          la CNIL :{' '}
          <a href="https://www.cnil.fr" rel="noopener noreferrer" className={TEXT_LINK}>
            www.cnil.fr
          </a>
          .
        </p>
      </LegalSection>

      <LegalSection title="Ton âge">
        <p>
          Recallio s’adresse à des adultes et à des étudiants : il ne s’adresse pas aux moins de 15
          ans, l’âge à partir duquel on consent seul, en France, à un service en ligne. Recallio ne
          vérifie pas l’âge de ses utilisateurs.
        </p>
      </LegalSection>

      <LegalSection title="Quand cette page change">
        <p>
          La date en haut de la page est celle de sa dernière modification. Le texte fait partie du
          code source de Recallio, qui est public : ses versions successives y restent lisibles.
        </p>
      </LegalSection>
    </LegalPage>
  )
}
