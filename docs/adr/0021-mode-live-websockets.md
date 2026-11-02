# 0021. Mode live : WebSockets Next, état des parties en mémoire, jeton dans le sous-protocole

- Statut : accepté
- Date : 2026-11-02

## Contexte

La phase 3 apporte le mode live (CLAUDE.md, section 6.5) : une partie synchronisée façon quiz, où l'hôte lance, les
membres rejoignent et voient tout en direct. Il faut un canal du serveur vers les navigateurs, un endroit où vivent les
parties en cours, et une authentification du canal. Cette première étape pose la salle d'attente ; le déroulé des
questions, les points et la robustesse suivent.

## Options envisagées

Pour le canal temps réel :

1. **WebSockets Next** (retenu), l'extension Quarkus prévue par la stack (section 3) : bidirectionnelle, intégrée à la
   sécurité et à CDI.
2. Server-Sent Events : plus simple, mais à sens unique ; les réponses des joueurs passeraient par des requêtes REST à
   côté, et la synchronisation serait plus difficile à suivre.

Pour l'état des parties :

1. **En mémoire de l'instance** (retenu), derrière un port (`LiveGameRepository`) : une partie dure quelques minutes,
   l'application tourne sur une seule instance (monolithe, ADR 0001). Seuls les résultats finaux iront en base.
2. Valkey dès maintenant : nécessaire avec plusieurs instances, prématuré avec une seule (YAGNI). Le port permet de
   l'introduire sans toucher au reste.

Pour authentifier le WebSocket (un navigateur ne peut pas lui ajouter d'en-tête `Authorization`) :

1. **Le jeton dans le sous-protocole** (retenu) : la PWA ouvre le WebSocket avec les protocoles `bearer-token-carrier` et
   `quarkus-http-upgrade#Authorization#Bearer <jeton>` (encodé) ; Quarkus replace ce dernier en en-tête avant la
   vérification habituelle du JWT. C'est le mécanisme documenté par Quarkus, sans code de sécurité maison.
2. Le jeton dans l'URL (`?token=`) : il finirait dans les journaux d'accès du proxy.
3. Un ticket à usage unique obtenu en REST : sûr, mais c'est du code de sécurité à écrire et à maintenir.

## Décision

- Un module **`live`** (section 4.1), qui lit le module group par ses API publiques (`GroupMembers`,
  `GroupReviewScopes`) et le module user par `UserDirectory`.
- **Tout membre** du groupe peut ouvrir une partie, sur l'un des sujets du groupe (422 `live.subject-not-in-group`
  sinon) ; il en est l'hôte. Un groupe a **au plus une partie ouverte** (409 `live.game-already-open`) : on rejoint celle
  qui existe. 50 joueurs au plus (`live.game-full`).
- REST : `GET` et `POST /api/v1/groups/{id}/live-games`, `GET /api/v1/live-games/{id}`. Direct :
  `ws(s)://…/api/v1/live/{id}`. À chaque arrivée ou départ, le serveur diffuse l'état de la partie
  (`{"type": "game", "game": {…}}`) à toutes ses connexions.
- Un refus à l'ouverture du WebSocket ferme la connexion avec un code applicatif calqué sur HTTP : **4404** (partie
  inconnue ou d'un autre groupe), **4409** (conflit, partie complète), **4422** (autre refus).
- Un joueur peut avoir plusieurs connexions (deux onglets) : il reste dans la partie tant qu'il en garde une.
- Une salle se **ferme** quand son dernier joueur part ; une salle restée vide cinq minutes est abandonnée et ne bloque
  plus le groupe.
- nginx relaie le changement de protocole (`Upgrade`, `Connection`) et laisse une connexion silencieuse ouverte une
  heure ; Vite fait de même en développement.

## Conséquences

- Un redémarrage du backend ferme les parties en cours : acceptable pour des parties de quelques minutes, et la PWA
  propose de se reconnecter.
- Passer à plusieurs instances demandera un état partagé (Valkey) et une diffusion entre instances : à décider, par un
  nouvel ADR, si le besoin apparaît.
- Les étapes suivantes ajoutent des types de messages (`question`, `reveal`, `scores`…) à côté de `game`, sans changer le
  canal.
