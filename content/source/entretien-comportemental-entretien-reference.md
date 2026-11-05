---
subject: entretien-comportemental
title: Entretien comportemental
category: fundamentals
position: 210
status: draft
---

# Entretien comportemental : le document de référence pour les entretiens techniques

> Pour préparer un entretien : questions comportementales et présentation de son expérience.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. QUESTIONS COMPORTEMENTALES & PRÉSENTER SON EXPÉRIENCE
═══════════════════════════════════════════

### Vue d'ensemble

Un entretien technique n'est jamais purement technique. L'évaluateur cherche aussi à savoir comment tu travailles : comment tu réagis à un incident, à un désaccord, à un échec, comment tu apprends, si tu sais expliquer ce que tu as fait. Ces questions se préparent autant que les questions de Java, avec **tes propres exemples**.

Ce chapitre ne contient volontairement aucune expérience personnelle : il donne les trames de réponse. À toi de préparer, pour chaque trame, un ou deux exemples réels tirés de tes projets, de ton alternance, de tes stages ou de projets personnels. Un exemple vécu, précis et honnête vaut dix définitions.

### Cours

**La méthode STAR** pour toute question qui commence par "parle-moi d'une fois où..." :
- **Situation** : le contexte en deux phrases (projet, équipe, enjeu).
- **Tâche** : ce qui t'incombait à toi.
- **Action** : ce que **tu** as fait, concrètement (le "je", pas le "on"), avec les choix et leurs raisons.
- **Résultat** : ce qui en est sorti, chiffré si possible, et ce que tu en as retenu.

Variante utile : **STAR + L** (*Learning*), en terminant par ce que tu ferais différemment.

**Se présenter en deux minutes** (le "présente-toi" d'ouverture) : présent (ton rôle, ta stack, ce que tu fais aujourd'hui) → passé (le parcours qui y mène, en une ou deux étapes marquantes) → futur (ce que tu cherches et pourquoi ce poste y correspond). Ne pas réciter le CV, que l'interlocuteur a sous les yeux.

**Présenter un projet** : contexte métier en une phrase, architecture en une phrase (et un schéma si un tableau est disponible), **ton périmètre exact**, la difficulté principale, comment tu l'as résolue, le résultat et une limite assumée. Prépare-toi aux relances : "pourquoi ce choix ?", "qu'aurais-tu fait autrement ?", "comment l'as-tu testé ?", "combien de requêtes par seconde ?". Ne cite pas une technologie que tu ne saurais pas expliquer.

**Les règles d'or** :
- **Précision sur le périmètre** : distinguer ce que tu as fait de ce que l'équipe a fait.
- **Honnêteté** : un résultat nuancé honnêtement inspire plus confiance qu'un succès survendu ; un évaluateur expérimenté détecte l'exagération en deux relances.
- **Concision** : deux minutes par réponse, puis laisser l'interlocuteur creuser.
- **Jamais de critique** d'un ancien employeur ou d'anciens collègues : décrire une situation, pas accuser des personnes.
- **Le "je ne sais pas"** est acceptable s'il est suivi de "voici comment je chercherais".

**Questions à préparer**, avec la trame attendue :
| Question | Ce qui est évalué | Trame |
|---|---|---|
| Ta plus grande difficulté technique | démarche de résolution | STAR, insister sur le diagnostic et la méthode |
| Un échec ou une erreur | lucidité, apprentissage | reconnaître sa part, la correction, ce qui a changé ensuite |
| Un conflit ou désaccord avec un collègue | communication | écouter, argumenter sur des faits, trouver une issue, relation préservée |
| Un désaccord avec ton lead | courage et loyauté | exprimer son avis argumenté, puis appliquer la décision prise (*disagree and commit*) |
| Une urgence en production | sang-froid, priorités | rétablir d'abord, communiquer, analyser ensuite, post-mortem |
| Apprendre vite une techno inconnue | autonomie | documentation officielle, petit prototype, demander aux bonnes personnes |
| Une tâche floue ou mal spécifiée | proactivité | poser des questions, reformuler par écrit, proposer une première version |
| Un retour négatif reçu | ouverture | l'accepter, l'appliquer, exemple concret de progression |
| Une réalisation dont tu es fier | motivation, impact | un projet où ton apport est clair et mesurable |
| Prioriser plusieurs demandes | organisation | critères (impact, urgence), transparence sur les arbitrages |
| Travailler avec un code legacy | pragmatisme | comprendre avant de changer, tests de caractérisation, petits pas |

**Questions de motivation** :
- *Pourquoi ce poste / cette entreprise ?* : relier un élément concret de l'offre ou de l'entreprise (stack, domaine métier, taille, culture) à ce que tu cherches. Se renseigner avant.
- *Pourquoi quitter ton poste actuel ?* : formuler vers l'avant (ce que tu cherches) plutôt que contre (ce que tu fuis).
- *Où te vois-tu dans 3 ou 5 ans ?* : une direction crédible (expertise technique, responsabilités d'architecture, encadrement) sans promettre ce que tu ne sais pas encore.
- *Tes qualités et défauts ?* : une qualité prouvée par un exemple ; un vrai défaut, mineur pour le poste, avec ce que tu fais pour le compenser.
- *Prétentions salariales* : connaître la fourchette du marché pour le poste, le niveau et la région, et donner une fourchette argumentée.

**Spécificités d'un entretien en ESN** : le recruteur évalue aussi ta capacité à être placé chez un client. Questions fréquentes : mobilité géographique, ouverture à différents secteurs et technologies, autonomie chez un client, façon de représenter l'entreprise, réaction à une période d'*intercontrat*. Les entretiens se font souvent en deux temps : avec l'ESN, puis avec le client final (qui pose les questions techniques sur ses propres sujets).

**Le test technique à la maison** : lire l'énoncé deux fois, poser les questions de clarification, livrer un code **propre et testé** plutôt qu'exhaustif, avec un README qui explique les choix, les hypothèses et ce qui aurait été fait avec plus de temps. La revue de ce test en entretien est l'occasion de montrer ta capacité à défendre et critiquer ton propre code.

**Les questions à poser à la fin** (toujours en avoir deux ou trois) : comment se passe une journée type dans l'équipe ? Comment se fait la revue de code et la mise en production ? Quelle est la plus grosse difficulté technique actuelle ? Comment est gérée la dette technique ? Comment s'organise la montée en compétence d'un nouvel arrivant ? Qu'est-ce qui distingue un bon développeur dans cette équipe ? Quelles sont les prochaines étapes du processus ?

**Communication pendant l'entretien technique** : penser à voix haute, cadrer avant de répondre, structurer ("trois points : ..."), donner un exemple concret pour chaque concept, admettre une incertitude, et vérifier que la réponse correspond à la question ("est-ce que je réponds à ce que vous vouliez savoir ?").

### En situation

- **"Présente-toi"** → présent, passé, futur, deux minutes.
- **"Parle-moi d'une fois où..."** → STAR, avec un exemple préparé.
- **"Pourquoi nous ?"** → un élément concret de l'entreprise relié à ton projet.
- **"As-tu des questions ?"** → toujours oui.

### Questions techniques

**Q : C'est quoi la méthode STAR ?** R : Situation, Tâche, Action, Résultat : une structure pour raconter une expérience concrète.

**Q : Combien de temps doit durer une présentation de soi ?** R : Environ deux minutes, en suivant présent, passé, futur.

**Q : Comment parler d'un échec ?** R : Le reconnaître, expliquer sa part de responsabilité, ce qui a été corrigé et ce qui a changé ensuite.

**Q : C'est quoi "disagree and commit" ?** R : Exprimer son désaccord argumenté, puis s'engager pleinement dans la décision prise.

**Q : Que faire si on ne connaît pas la réponse à une question ?** R : Le dire, puis expliquer comment on chercherait ou comment on raisonnerait.

**Q : Que contient un bon README de test technique ?** R : Comment lancer, les choix et hypothèses, les limites, et ce qui serait amélioré avec plus de temps.

### Questions de situation

**Q : "Parle-moi d'un problème technique difficile que tu as résolu."** *(trame)*
R : "[**Situation** : le système, l'enjeu, le symptôme observé, en deux phrases.] [**Tâche** : ce qu'on attendait de moi.] [**Action** : comment j'ai cherché, en insistant sur la démarche : reproduire, mesurer, émettre une hypothèse, la vérifier ; les fausses pistes écartées ; la solution retenue et pourquoi.] [**Résultat** : l'effet mesurable, et ce que j'en ai tiré, par exemple une meilleure compréhension d'un mécanisme ou un outil de diagnostic que j'utilise depuis.]"

**Q : "Comment réagis-tu si on te confie une tâche sur une technologie que tu ne connais pas, avec une échéance courte ?"**
R : "Je commencerais par cadrer ce qui est vraiment nécessaire pour l'échéance, puis par la documentation officielle et un prototype minimal pour valider le cœur du besoin rapidement. J'identifierais qui, dans l'équipe ou l'entreprise, connaît cette technologie pour lever les blocages vite plutôt que de rester bloqué seul.
Et je serais transparent sur les risques dès le début : si l'échéance me paraît intenable, je préfère le signaler tôt avec une proposition (réduire le périmètre, être épaulé) que de découvrir le retard à la fin."

**Q : "En entretien, on te montre un code et tu n'es pas sûr de la réponse."**
R : "Je raisonnerais à voix haute en appliquant les règles que je connais : 'cette expression est un int parce que...', 'cette concaténation est évaluée à la compilation parce que les deux opérandes sont des constantes'. Même si je me trompe sur le résultat final, l'évaluateur voit que je connais les mécanismes.
Et si on me demande ce que je ferais en vrai : je l'exécuterais dans `jshell` en dix secondes. Le but de ces questions n'est pas de remplacer un compilateur, mais de vérifier qu'on reconnaît les situations dangereuses quand on les rencontre dans une revue de code."

═══════════════════════════════════════════
# GLOSSAIRE DES SIGLES ET TERMES
═══════════════════════════════════════════

| Terme | Signification |
|---|---|
| **ESN** | Entreprise de Services du Numérique |
| **STAR** | Situation, Tâche, Action, Résultat |
