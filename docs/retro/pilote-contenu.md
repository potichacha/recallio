# Rétrospective : pilote du contenu (phase 3 bis, étape 6)

- Période : 2026-11-03
- Livré : PR #58 à #60, et celle de ce bilan

## Ce qui a été produit

Les trois premiers chapitres du parcours d'un débutant en Java, complets pour la priorité 1 :

| Chapitre | Notions P1 | Cartes visées | Exercice |
|---|---|---|---|
| Premiers pas | 6 | 116 | `saluer` (chaînes, `null`, `strip`) |
| Types et variables | 8 | 184 | `moyenne` (division entière) |
| Opérateurs | 6 | 144 | `duree` (`/` et `%`) |
| **Total** | **20** | **444** | **3** |

Soit 20 leçons, 444 cartes à la cible (4 par format retenu), dont 47 « qu'affiche ce code ? » exécutés par la CI et 3
exercices joués dans le bac à sable. Java est passé de 573 à 992 cartes. Les 20 notions sont déclarées complètes
(`complete: true`) : la CI vérifie désormais qu'elles le restent.

## Ce qui a bien marché

- **Restreindre les formats par notion, avant d'écrire.** Sans restriction, une notion vise 8 formats, soit 32 cartes.
  Le pilote en a retenu 4 à 6, ceux qui ont un sens (pas de mise en situation sur `var`, pas de définition sur la
  portée), ce qui ramène les cibles d'un quart à un tiers plus bas, sans cartes de remplissage.
- **La cible de 4 cartes par format tient.** Quatre angles par format suffisent à varier les révisions d'une notion sur
  plusieurs jours, avec une seule carte par jour (ADR 0031) : on la garde.
- **L'exécution du code en CI est un filet efficace.** Les 47 « qu'affiche ce code ? » et les 3 exercices sont vérifiés
  à chaque PR ; aucune sortie annoncée n'était fausse.
- **La leçon en un écran** (l'idée, un exemple, le piège, la phrase d'entretien) se lit en une minute et se réutilise
  telle quelle d'une notion à l'autre.

## Ce qui a coincé

- **Des erreurs de fond que seule la relecture attrape.** J'en ai trouvé deux dans mes propres brouillons, sur des faits
  que la CI ne vérifie pas :
  - une méthode `main` sans `static` est acceptée depuis Java 25 (méthodes `main` d'instance) : la leçon et trois
    cartes affirmaient le contraire, corrigées après vérification sur le JDK 25 ;
  - « on ne distribue plus de JRE séparé » n'est vrai que pour Oracle : Temurin en publie encore.
  Les vrai ou faux, les QCM et les mises en situation énoncent des faits que rien n'exécute : **la relecture par le
  propriétaire (ADR 0025) reste indispensable**, et les PR #58 à #60 ont été fusionnées sans commentaire.
- **Le YAML et la typographie française font mauvais ménage.** Les « : » et les virgules dans les choix de QCM cassent
  le format ; un normaliseur (guillemets là où YAML les exige, choix en blocs) a été nécessaire avant chaque PR.
- **L'ordre des nouvelles cartes a changé un parcours de bout en bout.** Un nouvel exercice de priorité 1 est passé devant
  celui qu'attendait le test : le test cible maintenant un thème, plutôt que de dépendre de l'ordre du contenu.

## Ce qui reste à produire

| Priorité | Notions | Déclarées complètes | Cartes restantes (formats restreints) | Leçons restantes |
|---|---|---|---|---|
| 1 | 168 | 20 | ~3 200 | 148 |
| 2 | 105 | 0 | ~2 250 | 105 |
| 3 | 58 | 0 | ~1 200 | 58 |
| 4 | 16 | 0 | ~340 | 16 |

Rien que la priorité 1 de Java représente sept fois le pilote : une quarantaine de chapitres, soit une vingtaine de PR
de contenu au rythme de deux chapitres par PR.

## Ce qu'on change

1. **Les formats d'une notion se restreignent dans le référentiel avant de rédiger**, et c'est la première chose
   à relire dans une PR de contenu.
2. **Une notion terminée se déclare `complete: true`** : la matrice devient bloquante pour elle (fait dans ce bilan).
3. **La relecture des faits ne peut pas être sautée.** Je signalerai, dans chaque PR, les affirmations les plus
   délicates à vérifier en priorité (versions de Java, comportements récents).
4. **La production de la priorité 1 de Java continue en parallèle des fonctionnalités** (CLAUDE.md, section 5.6). Le
   critère de fin de la phase 3 bis (un débutant suit le parcours Java de bout en bout sur la priorité 1) dépend d'elle,
   pas du code, qui est prêt.

## Question ouverte

Faut-il garder 4 cartes par format pour les priorités 2 à 4 ? À ce rythme, Java complet dépasse 7 000 cartes. Une
cible de 2 ou 3 cartes par format pour les notions avancées ou rares diviserait cet effort, au prix de révisions moins
variées sur ces notions. La décision peut attendre la fin de la priorité 1.
