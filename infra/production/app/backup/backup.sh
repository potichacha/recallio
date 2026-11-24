#!/usr/bin/env bash
# La sauvegarde des deux bases du serveur de l'application (ADR 0073) : recallio et keycloak, chacune par pg_dump au
# format personnalisé, chiffrée avec age pour une clé publique, puis envoyée hors du serveur par rclone.
#
# Lancé chaque nuit par recallio-backup.timer, et à la main par :
#
#   sudo systemctl start recallio-backup.service
#   journalctl -u recallio-backup.service -n 20
#
# La configuration vient de l'environnement, que recallio-backup.service remplit depuis /etc/recallio-backup/backup.env
# (modèle : backup.env.example) et /etc/recallio-backup/rclone.conf (modèle : rclone.conf.example).
#
# Ce que le script garantit :
#   - aucun dump en clair sur le disque : pg_dump écrit dans age, et seul le fichier chiffré est posé, dans un dossier
#     de travail effacé à la sortie, réussie ou non ;
#   - rien n'est envoyé tant que les deux bases ne sont pas sauvegardées en entier : une sauvegarde interrompue ne
#     laisse aucun fichier tronqué à côté des bons ;
#   - chaque fichier envoyé est retrouvé dans la destination, à sa taille, avant d'être annoncé ;
#   - le battement de cœur n'est appelé qu'après tout cela : son silence veut dire qu'une sauvegarde manque.
#
# Le serveur ne sait que chiffrer et déposer : la clé privée d'age n'y est jamais, et la clé du stockage ne permet ni de
# lire ni de supprimer. La restauration est dans docs/deploiement.md, section 12.

set -euo pipefail

# Les deux bases de l'instance (postgres/init-databases.sql). Les rôles ne sont pas sauvegardés : le script
# d'initialisation les recrée sur un serveur neuf, avec les mots de passe du .env.
readonly DATABASES=(recallio keycloak)

log() {
  printf 'sauvegarde : %s\n' "$*"
}

fail() {
  printf 'sauvegarde : ÉCHEC : %s\n' "$*" >&2
  exit 1
}

trap 'printf "sauvegarde : ÉCHEC à la ligne %s : la sauvegarde de cette nuit est à refaire\n" "$LINENO" >&2' ERR

# --- La configuration -------------------------------------------------------------------------------------------------

# La clé publique d'age (age1...) : ce qui est chiffré pour elle ne se lit qu'avec la clé privée, restée hors du serveur.
: "${BACKUP_AGE_RECIPIENT:?la clé publique age du destinataire (age1...) manque}"
# La destination, pour rclone : <remote>:<bucket>/<dossier>. Le script ne connaît pas le fournisseur.
: "${BACKUP_DESTINATION:?la destination rclone (remote:bucket/dossier) manque}"
# Facultative : l'adresse appelée après une sauvegarde réussie. Vide, rien n'est appelé.
BACKUP_HEARTBEAT_URL=${BACKUP_HEARTBEAT_URL:-}

for tool in docker age rclone; do
  command -v "$tool" > /dev/null || fail "la commande $tool est introuvable"
done
if [[ -n $BACKUP_HEARTBEAT_URL ]]; then
  command -v curl > /dev/null || fail "la commande curl est introuvable"
fi

# Le dossier du compose de production : le parent de celui du script.
compose_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$compose_dir"

# --- Le dossier de travail --------------------------------------------------------------------------------------------

# Lisible par root seul, et effacé à la sortie. Il ne reçoit que des fichiers chiffrés. systemd donne STATE_DIRECTORY
# (/var/lib/recallio-backup), sur le disque : /tmp peut être en mémoire.
umask 077
work_dir=$(mktemp -d "${STATE_DIRECTORY:-${TMPDIR:-/tmp}}/run.XXXXXXXX")
trap 'rm -rf -- "$work_dir"' EXIT

# L'heure UTC du lancement, la même pour les deux fichiers : recallio-2026-11-24T0215Z.dump.age.
stamp=$(date -u +%Y-%m-%dT%H%MZ)

# --- Sauvegarder et chiffrer ------------------------------------------------------------------------------------------

log "début, vers ${BACKUP_DESTINATION}"
for database in "${DATABASES[@]}"; do
  file="$work_dir/$database-$stamp.dump.age"
  # pipefail : un pg_dump qui échoue fait échouer la ligne, même si age a chiffré ce qu'il a reçu.
  docker compose exec -T postgres pg_dump --username postgres --no-password --format custom "$database" \
    | age --encrypt --recipient "$BACKUP_AGE_RECIPIENT" --output "$file"
  log "base $database sauvegardée et chiffrée ($(stat --format %s "$file") octets)"
done

# --- Envoyer et vérifier ----------------------------------------------------------------------------------------------

for database in "${DATABASES[@]}"; do
  name="$database-$stamp.dump.age"
  local_size=$(stat --format %s "$work_dir/$name")
  # --no-check-dest : rclone n'interroge pas la destination avant d'envoyer, ce que la clé du stockage ne permet pas.
  # L'envoi porte l'empreinte MD5 du fichier, que le stockage vérifie à la réception.
  rclone copyto --no-check-dest "$work_dir/$name" "$BACKUP_DESTINATION/$name"
  # La liste du dossier, seule lecture permise à la clé : le fichier doit y être, à sa taille.
  remote_size=$(rclone lsf --files-only --format s --include "/$name" "$BACKUP_DESTINATION/")
  if [[ $remote_size != "$local_size" ]]; then
    fail "$name fait $local_size octets ici, et « $remote_size » dans la destination"
  fi
  log "$name envoyé et retrouvé dans la destination ($remote_size octets)"
done

# --- Le battement de cœur ---------------------------------------------------------------------------------------------

# Une requête sans contenu. Si elle échoue, la sauvegarde reste bonne : le service de surveillance, qui n'aura rien
# reçu, alertera de lui-même. L'adresse, qui vaut identifiant, n'est jamais écrite dans le journal.
if [[ -n $BACKUP_HEARTBEAT_URL ]]; then
  if curl --fail --silent --max-time 10 --retry 3 --output /dev/null "$BACKUP_HEARTBEAT_URL"; then
    log "battement de cœur envoyé"
  else
    log "ATTENTION : le battement de cœur n'a pas pu être envoyé (la sauvegarde, elle, est faite)"
  fi
fi

log "terminé : ${#DATABASES[@]} bases sauvegardées"
