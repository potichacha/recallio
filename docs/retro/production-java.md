# Rétrospective : production du contenu Java (phase 3 bis, étape 6)

- Période : 2026-11-03 au 2026-11-04
- Livré : une seule PR de contenu (branche `content/java-complete`), à la suite du pilote (`pilote-contenu.md`)

## Ce qui a été produit

Toutes les notions du référentiel Java, toutes priorités confondues, sont déclarées complètes (`complete: true`) :

| Priorité | Notions | Complètes |
|---|---|---|
| 1 | 168 | 168 |
| 2 | 105 | 105 |
| 3 | 58 | 58 |
| 4 | 16 | 16 |
| **Total** | **347** | **347** |

- **7 072 cartes**, contre 992 à la fin du pilote ;
- une leçon par notion (347) ;
- 963 « qu'affiche ce code ? », tous exécutés par JShell en CI ;
- 39 exercices de code, joués dans le bac à sable (le code de départ échoue, la solution passe) ;
- de nouvelles notions dans le référentiel, là où le document d'origine avait des trous : `packages-imports`,
  `classpath`, `jar`, `junit-exceptions`, `junit-organisation`, `assertj`, `annotations-standard`, `reflexion-bases`,
  `reflexion-annotations`, `method-handles`, et les notions de bonnes pratiques (`gestion-null`, `minimiser-mutabilite`,
  `nommage-lisibilite`, `odeurs-code-java`, `analyse-statique`).

## Réponse à la question ouverte du pilote

**4 cartes par format, partout**, priorités 2 à 4 comprises (choix du propriétaire). Les notions avancées ou rares sont
celles qu'on oublie le plus vite : des révisions variées y sont autant utiles qu'ailleurs. Le prix est assumé : plus de
7 000 cartes à relire.

## Ce qui a bien marché

- **L'exécution du code en CI** a attrapé chaque erreur de sortie annoncée, avant le merge (voir plus bas les pièges
  JShell) ; aucune carte de code n'a besoin d'être relue pour sa sortie.
- **Les scripts de normalisation** (formats renseignés, `complete: true`, guillemets YAML) ont rendu le travail
  mécanique ; le fond seul demande de l'attention.
- **Un compte rendu par domaine**, avec un commit de sauvegarde, a gardé la PR géante découpée en morceaux relisibles
  dans l'historique.

## Ce qui a coincé

- **Les pièges de JShell**, désormais documentés dans `content/README.md` :
  - les imports hors des valeurs par défaut (atomic, locks, reflect...) ;
  - l'ambiguïté de `Proxy` entre `java.net` et `java.lang.reflect` ;
  - le constructeur compact d'un record, qui doit être `public` ;
  - les types scellés, à déclarer dans une classe englobante ;
  - `toString` appelé par JShell, qui faussait un compteur ;
  - les interruptions, que le thread principal de JShell gère lui-même.
- **Le YAML** : des clés interdites (`null`, majuscules), des lignes parasites, et des « : » à mettre entre guillemets
  après une correction tardive.
- **La règle des exercices** : un exercice n'est exigé dans un chapitre que si l'une de ses notions retient le format
  `exercise` (sinon un chapitre de pure théorie, comme l'histoire de Java, ne pourrait jamais être complet). La règle
  était dans le code (`ContentCoverage`) ; elle est maintenant écrite dans le guide.

## Faits à relire en priorité

Les cartes ci-dessous énoncent des faits que rien n'exécute, sur des sujets récents ou mouvants :

- **Java 26 et 27** : `apporte-java-27`, `gc-defaut-java-27`, `compact-object-headers`, HTTP/3 dans le client HTTP
  (Java 26) ;
- **`StructuredTaskScope`** : l'API est en préversion et a changé d'une version à l'autre ;
- **le cache AOT** (JEP 483 puis 514/515) : la forme des options selon Java 24 ou 25 ;
- **les seuils des ramasse-miettes**, donnés de mémoire (tailles de régions G1, pauses visées) ;
- **jlink** : `jdk.crypto.ec` est fusionné dans `java.base` depuis le JDK 22 (corrigé pendant la production, à
  confirmer) ;
- **les versions** citées dans les exemples de POM et de plugins ;
- **la cryptographie** (algorithmes et paramètres recommandés).

## Ce qu'on change

1. **Go suit le même chemin** : référentiel, dossier de notions, 4 cartes par format, exercices jugés par des tests.
   Cela demande un bac à sable Go et un runner multi-langage, à décider dans un ADR avant le contenu.
2. **Les pièges d'un format se documentent dès qu'on les rencontre**, dans `content/README.md`, plutôt qu'en fin de
   production.
3. **La relecture du propriétaire reste la seule garantie sur les faits** (ADR 0025) : la liste ci-dessus en est le
   point de départ.
