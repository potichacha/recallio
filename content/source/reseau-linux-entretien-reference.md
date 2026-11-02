---
subject: reseau-linux
title: Réseau & Linux
category: fundamentals
position: 180
status: published
---

# Réseau & Linux : le document de référence pour les entretiens techniques

> Pour préparer un entretien : réseau et Linux pour les développeurs.
> Chaque chapitre suit la même structure : **Vue d'ensemble** (le pourquoi) → **Cours** (le détail) → **En situation** (les ponts entre symptômes et notions) → **Questions techniques** (réponses courtes) → **Questions de situation** (réponses rédigées).
> Ces chapitres viennent du document Java, dont ils ont été sortis pour devenir un sujet à part entière (ADR 0025).

═══════════════════════════════════════════
# 1. RÉSEAU & LINUX
═══════════════════════════════════════════

### Vue d'ensemble

Sur un poste DevOps, on attend une compréhension du **chemin complet** d'une requête : du navigateur au processus applicatif. Sans cette vision, beaucoup d'incidents restent inexplicables, un timeout n'a pas la même cause selon qu'il vient du DNS, de la poignée de main TLS, d'un pare-feu ou de l'application elle-même.

Côté Linux, il ne s'agit pas d'être administrateur système, mais d'être **autonome pour diagnostiquer** : voir ce qui tourne, ce qui écoute, ce qui consomme, et lire les logs.

### Cours

**Le chemin d'une requête** : résolution **DNS** (nom → IP) → établissement **TCP** (poignée de main en trois temps) → négociation **TLS** si HTTPS → requête **HTTP** → réponse.

**TCP vs UDP** : TCP garantit l'ordre et la livraison, avec contrôle de congestion, c'est la base du web. UDP n'offre aucune garantie mais est plus léger, utilisé pour le DNS, la vidéo, les jeux.

**HTTP** : `1.1` (une requête à la fois par connexion, d'où le keep-alive et les connexions multiples), `2` (multiplexage sur une seule connexion, compression des en-têtes), `3` (basé sur QUIC/UDP, meilleure résistance à la perte de paquets).

**En-têtes utiles** : `Content-Type`, `Authorization`, `Cache-Control`, `ETag` (validation de cache), `X-Forwarded-For` (IP d'origine derrière un proxy).

**TLS** : chiffre le transport et authentifie le serveur via un certificat signé par une autorité. **mTLS** ajoute l'authentification du client. Erreurs courantes : certificat expiré, nom qui ne correspond pas, chaîne de certification incomplète.

**CORS** : mécanisme du navigateur empêchant une page d'un domaine d'appeler une API d'un autre domaine sans autorisation explicite. C'est une protection **côté navigateur** : elle n'existe pas pour un appel serveur à serveur, ce qui explique qu'un `curl` fonctionne là où le navigateur refuse.

**Load balancing** : réparti selon différentes stratégies (tourniquet, moins de connexions, hachage d'IP). Peut être en **couche 4** (TCP, rapide, aveugle au contenu) ou **couche 7** (HTTP, capable de router selon le chemin ou les en-têtes).

**Proxy vs reverse proxy** : le premier agit pour le client, le second devant les serveurs (terminaison TLS, répartition, cache).

**Commandes Linux essentielles :**
```bash
ps aux | grep java          # processus en cours
top / htop                  # consommation temps réel
df -h / du -sh *            # espace disque
free -h                     # mémoire
netstat -tulpn / ss -tulpn  # ports en écoute
curl -v https://...         # requête HTTP détaillée
dig / nslookup              # résolution DNS
tail -f fichier.log         # suivre un log en direct
grep -r "erreur" .          # rechercher
journalctl -u service       # logs systemd
kill -15 PID                # arrêt propre (SIGTERM)
```

**Permissions** : `rwx` pour propriétaire/groupe/autres, `chmod`, `chown`. Sur un conteneur non-root, un problème d'écriture est presque toujours une histoire de permissions.

**Diagnostic Java sur un serveur** : `jps` (processus JVM), `jstack` (thread dump), `jmap` (heap dump), `jstat` (statistiques GC).

**Modèle OSI** (7 couches) et modèle TCP/IP :
| Couche OSI | Exemples |
|---|---|
| 7 Application | HTTP, DNS, SMTP, gRPC |
| 6 Présentation | encodage, chiffrement (TLS est souvent placé entre 4 et 7) |
| 5 Session | gestion des sessions |
| 4 Transport | TCP, UDP, QUIC |
| 3 Réseau | IP, ICMP (ping), routage |
| 2 Liaison | Ethernet, MAC, switchs |
| 1 Physique | câbles, Wi-Fi |

**Adressage IP** : IPv4 sur 32 bits, notation **CIDR** (`10.0.0.0/24` = 256 adresses, `/16` = 65 536), plages **privées** `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `127.0.0.1` (boucle locale), `0.0.0.0` (toutes les interfaces : une application qui écoute sur `127.0.0.1` dans un conteneur n'est pas joignable de l'extérieur, il faut écouter sur `0.0.0.0`). **NAT** : traduction d'adresses privées vers une adresse publique. IPv6 sur 128 bits. Ports : 0-1023 réservés (22 SSH, 53 DNS, 80 HTTP, 443 HTTPS, 5432 PostgreSQL, 3306 MySQL, 6379 Redis, 9092 Kafka).

**TCP, compléments** : poignée de main SYN → SYN-ACK → ACK ; fermeture par FIN ; l'état **TIME_WAIT** retient le port quelque temps après fermeture (beaucoup de connexions courtes épuisent les ports, d'où l'intérêt du keep-alive et des pools de connexions) ; *connection refused* (rien n'écoute sur le port, réponse RST immédiate) vs *connection timeout* (aucune réponse, souvent un pare-feu qui ignore les paquets) vs *read timeout* (connecté, mais la réponse tarde).

**DNS** : enregistrements **A** (nom → IPv4), **AAAA** (IPv6), **CNAME** (alias vers un autre nom), **MX** (mail), **TXT** (vérifications, SPF), **NS**, **SRV**. **TTL** : durée de mise en cache ; un changement DNS met jusqu'au TTL à se propager. La JVM met elle-même en cache les résolutions (`networkaddress.cache.ttl`), ce qui a longtemps posé problème quand une IP changeait.

**Temps réel et HTTP** : HTTP/1.1 keep-alive (connexion réutilisée), *head-of-line blocking* (une requête lente bloque les suivantes sur la connexion), HTTP/2 multiplexage, HTTP/3 sur QUIC. WebSocket : passage du protocole via `Upgrade`.

**Linux, compléments** :
- **Processus et signaux** : `SIGTERM` (15, demande d'arrêt, interceptable), `SIGKILL` (9, arrêt immédiat, non interceptable), `SIGINT` (2, Ctrl+C), `SIGHUP` (1, souvent rechargement de configuration), `SIGQUIT` (3, pour une JVM : produit un **thread dump** sur la sortie standard, avec `kill -3 <pid>`). Processus **zombie** (terminé mais pas encore récupéré par son parent), PID 1 dans un conteneur.
- **Descripteurs de fichiers** : chaque socket et fichier ouvert en consomme un ; limite par processus (`ulimit -n`) ; erreur `Too many open files` = fuite de ressources non fermées ou limite trop basse. `lsof -p <pid>` liste les fichiers et sockets ouverts.
- **Mémoire** : `free -h` (la colonne *available* compte, le cache disque est récupérable), **OOM killer** du noyau qui tue un processus quand la mémoire manque (visible dans `dmesg`), swap (désastreux pour une JVM, le GC parcourant des pages swappées).
- **Charge** : *load average* (`uptime`, `top`) = nombre moyen de processus prêts ou en attente d'I/O, à comparer au nombre de cœurs. `vmstat 1`, `iostat -x 1` (attente disque), `top -H -p <pid>` (threads d'un processus).
- **Texte** : `grep -i -r -n`, `less`, `tail -f`, `head`, `wc -l`, `sort | uniq -c | sort -rn` (compter les occurrences), `awk '{print $9}'` (une colonne), `sed 's/a/b/g'`, `cut`, `find . -name "*.log" -mtime +7 -delete`, `xargs`, `jq` (JSON).
- **Réseau** : `ss -tulpn`, `curl -v`/`-I`/`-w` (mesures de temps), `nc -zv hote port` (tester un port), `dig`/`nslookup`, `ip a`, `ping`, `traceroute`, `tcpdump`.
- **Permissions** : `chmod 755` (rwx pour le propriétaire, r-x pour groupe et autres ; r=4, w=2, x=1), `chown user:groupe`, `sudo`, umask.
- **Services** : `systemctl start|stop|status|enable service`, `journalctl -u service -f`, `cron` (`crontab -e`, format `minute heure jour mois jour_semaine`).
- **Distant** : `ssh user@hote`, clés SSH (`ssh-keygen`, `authorized_keys`), `scp`, `rsync`, tunnel `ssh -L 5432:db:5432 bastion`.
- **Variables d'environnement** : `export VAR=x`, `env`, `printenv`, `.bashrc` ; redirections `>` (écraser), `>>` (ajouter), `2>&1` (stderr vers stdout), `|` (pipe), `&` (arrière-plan), `nohup`.

### En situation

- **"L'appel échoue en timeout"** → DNS, TCP, TLS, application ?
- **"Ça marche en curl mais pas dans le navigateur"** → CORS.
- **"Le disque est plein"** → df, du, rotation des logs.
- **"Le service ne répond plus"** → ps, ports en écoute, thread dump.

### Questions techniques

**Q : Étapes d'une requête HTTPS ?** R : DNS → TCP (handshake) → TLS → HTTP.

**Q : TCP vs UDP ?** R : Fiable et ordonné vs léger sans garantie.

**Q : Apport d'HTTP/2 ?** R : Multiplexage sur une connexion et compression des en-têtes.

**Q : C'est quoi le CORS ?** R : Une protection du navigateur contre les appels cross-origin non autorisés, inexistante en serveur à serveur.

**Q : Load balancing L4 vs L7 ?** R : Répartition TCP aveugle au contenu vs répartition HTTP capable de router selon le chemin.

**Q : Proxy vs reverse proxy ?** R : Agit pour le client vs se place devant les serveurs.

**Q : Comment voir les ports en écoute ?** R : `ss -tulpn` ou `netstat -tulpn`.

**Q : Comment prendre un thread dump ?** R : `jstack <pid>`.

**Q : Que fait `kill -15` ?** R : Envoie SIGTERM, permettant un arrêt propre, contrairement à `kill -9`.

**Q : Les 7 couches OSI ?** R : Physique, liaison, réseau, transport, session, présentation, application.

**Q : À quelle couche se situent TCP, IP et HTTP ?** R : Transport, réseau, application.

**Q : Combien d'adresses dans un /24 ?** R : 256.

**Q : Les plages d'adresses privées ?** R : 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16.

**Q : Pourquoi une application qui écoute sur 127.0.0.1 dans un conteneur est-elle injoignable ?** R : Elle n'écoute que sur la boucle locale du conteneur ; il faut 0.0.0.0.

**Q : Connection refused vs connection timeout ?** R : Rien n'écoute (rejet immédiat) vs aucune réponse (pare-feu, réseau, hôte injoignable).

**Q : C'est quoi TIME_WAIT ?** R : L'état d'une connexion fermée qui retient le port un moment ; trop de connexions courtes épuisent les ports.

**Q : Enregistrement A vs CNAME ?** R : Nom vers adresse IP vs alias vers un autre nom.

**Q : Pourquoi un changement DNS n'est-il pas immédiat ?** R : Les résolutions sont mises en cache pendant le TTL (y compris par la JVM).

**Q : SIGTERM vs SIGKILL ?** R : Demande d'arrêt interceptable vs arrêt immédiat non interceptable.

**Q : Que fait kill -3 sur une JVM ?** R : Il produit un thread dump sur la sortie standard sans arrêter le processus.

**Q : Que signifie "Too many open files" ?** R : La limite de descripteurs est atteinte, souvent à cause de ressources non fermées.

**Q : Comment voir les fichiers et sockets ouverts par un processus ?** R : lsof -p <pid>.

**Q : Comment interpréter le load average ?** R : Nombre moyen de processus actifs ou en attente d'I/O, à comparer au nombre de cœurs.

**Q : Que signifie chmod 644 ?** R : Lecture/écriture pour le propriétaire, lecture pour le groupe et les autres.

**Q : Comment compter les adresses IP les plus fréquentes dans un log d'accès ?** R : `awk '{print $1}' access.log | sort | uniq -c | sort -rn | head`.

**Q : Comment tester qu'un port distant est ouvert ?** R : `nc -zv hote port` ou `curl -v telnet://hote:port`.

**Q : Que fait 2>&1 ?** R : Redirige la sortie d'erreur vers la sortie standard.

### Questions de situation

**Q : "Un appel vers un service externe échoue en timeout. Comment tu remontes la chaîne ?"**
R : "Je testerais couche par couche, du bas vers le haut.
D'abord la **résolution DNS** avec `dig` : le nom se résout-il, et vers la bonne adresse ? Dans un cluster, une erreur de nom de service est fréquente.
Ensuite la **connectivité TCP** : est-ce que le port est joignable ? Un échec ici oriente vers un pare-feu ou une NetworkPolicy.
Puis **TLS** avec `curl -v` : certificat expiré, nom non concordant, chaîne incomplète ?
Enfin la couche **HTTP** : le service répond-il, avec quel code, en combien de temps ?
Si tout passe en `curl` depuis le pod mais échoue dans l'application, je regarde du côté de la configuration : mauvais port, proxy non pris en compte, ou timeout applicatif plus court que le temps de réponse normal du service.
La démarche générale : isoler la couche fautive avant de chercher une cause, sinon on tourne en rond."

**Q : "Le disque du serveur est plein en production."**
R : "En urgence, identifier ce qui occupe : `df -h` pour voir quelle partition, puis `du -sh *` en descendant l'arborescence pour localiser le coupable.
Les causes classiques : des **logs non pivotés** qui grossissent indéfiniment, des fichiers temporaires jamais nettoyés, ou des images et conteneurs Docker accumulés, `docker system prune` libère souvent beaucoup.
Attention à un piège : supprimer un fichier encore ouvert par un processus ne libère pas l'espace tant que le processus tourne. Il faut le redémarrer, ou tronquer le fichier plutôt que le supprimer.
Sur le fond, la vraie correction est la **rotation des logs** (logrotate), une politique de rétention, et une alerte sur le taux de remplissage avant d'atteindre 100%, parce qu'un disque plein en production provoque des pannes en cascade difficiles à diagnostiquer."

**Q : "Ton service lève des 'Too many open files' après quelques heures."**
R : "Une croissance progressive évoque une **fuite** plutôt qu'une limite mal réglée. Je regarderais `lsof -p <pid> | wc -l` dans le temps, puis le type des descripteurs : des sockets vers un même service (des clients HTTP créés à chaque appel et jamais fermés, des réponses dont le corps n'est pas consommé), des fichiers (des streams `Files.lines` non fermés).
Correction : try-with-resources systématique, un client HTTP partagé avec un pool, fermeture des réponses. Et en parallèle, une limite `ulimit -n` adaptée à la charge normale, avec une métrique et une alerte sur le nombre de descripteurs ouverts."

═══════════════════════════════════════════
# GLOSSAIRE DES SIGLES ET TERMES
═══════════════════════════════════════════

| Terme | Signification |
|---|---|
| **CDN** | Content Delivery Network : cache réparti géographiquement |
| **CIDR** | Notation d'une plage d'adresses IP (`10.0.0.0/24`) |
| **DNS** | Domain Name System : résolution de noms en adresses |
| **OSI** | Modèle réseau à sept couches |
