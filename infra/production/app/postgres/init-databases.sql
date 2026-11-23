-- Les deux bases du serveur de l'application et leurs propriétaires (ADR 0071) : une instance PostgreSQL, deux bases,
-- trois rôles. Le superutilisateur postgres (POSTGRES_PASSWORD) n'administre que l'instance ; recallio (le backend) et
-- keycloak ne sont ni superutilisateurs ni créateurs de bases ou de rôles, et chacun n'ouvre que sa propre base.
--
-- L'image officielle ne lance ce script qu'à la première initialisation, quand le volume des données est vide. Ensuite,
-- il ne s'exécute plus : changer un mot de passe dans le .env ne change donc pas celui de la base
-- (docs/deploiement.md).
--
-- Les mots de passe viennent des variables du conteneur, lues par psql (\getenv) : ni le dépôt ni la ligne de commande
-- ne les voient. Une variable absente laisse son nom dans la requête, qui échoue : l'initialisation s'arrête.

\set ON_ERROR_STOP on
\getenv recallio_password RECALLIO_DB_PASSWORD
\getenv keycloak_password KEYCLOAK_DB_PASSWORD

CREATE ROLE recallio LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'recallio_password';
CREATE ROLE keycloak LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'keycloak_password';

-- Propriétaire de sa base, chacun l'est aussi de son schéma public (pg_database_owner) : Flyway et Keycloak y créent
-- leurs tables.
CREATE DATABASE recallio OWNER recallio;
CREATE DATABASE keycloak OWNER keycloak;

-- Par défaut, tout rôle peut se connecter à toute base (PUBLIC) : seul le propriétaire le peut désormais.
REVOKE CONNECT, TEMPORARY ON DATABASE recallio FROM PUBLIC;
REVOKE CONNECT, TEMPORARY ON DATABASE keycloak FROM PUBLIC;
