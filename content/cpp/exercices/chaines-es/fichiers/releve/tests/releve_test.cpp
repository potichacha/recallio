#include "doctest.h"
#include "releve.hpp"

#include <filesystem>
#include <fstream>
#include <stdexcept>
#include <string>
#include <vector>

namespace {

// Un fichier du répertoire temporaire, supprimé à la fin du test.
struct FichierTemporaire {
    std::filesystem::path chemin;

    explicit FichierTemporaire(const std::string& nom)
        : chemin{std::filesystem::temp_directory_path() / nom} {
        std::filesystem::remove(chemin);
    }
    ~FichierTemporaire() {
        std::error_code erreur;
        std::filesystem::remove(chemin, erreur);
    }
};

void ecrire_brut(const std::filesystem::path& chemin, const std::string& contenu) {
    std::ofstream sortie{chemin};
    sortie << contenu;
}

const std::filesystem::path introuvable = "/dossier_absent_recallio/fichier.txt";

}  // namespace

TEST_CASE("écrit puis relit les mêmes lignes") {
    const FichierTemporaire fichier{"recallio_releve_1.txt"};
    const std::vector<std::string> lignes{"Ada 18", "Alan 15", ""};
    CHECK(ecrire_lignes(fichier.chemin, lignes));
    CHECK(lire_lignes(fichier.chemin) == lignes);
}

TEST_CASE("remplace le contenu existant") {
    const FichierTemporaire fichier{"recallio_releve_2.txt"};
    CHECK(ecrire_lignes(fichier.chemin, {"une", "deux", "trois"}));
    CHECK(ecrire_lignes(fichier.chemin, {"seule"}));
    CHECK(lire_lignes(fichier.chemin) == std::vector<std::string>{"seule"});
}

TEST_CASE("signale un fichier impossible à écrire") {
    CHECK_FALSE(ecrire_lignes(introuvable, {"a"}));
}

TEST_CASE("distingue un fichier vide d'un fichier absent") {
    const FichierTemporaire fichier{"recallio_releve_3.txt"};
    ecrire_brut(fichier.chemin, "");
    const auto vide = lire_lignes(fichier.chemin);
    REQUIRE(vide.has_value());
    CHECK(vide->empty());
    CHECK_FALSE(lire_lignes(introuvable).has_value());
}

TEST_CASE("lit une dernière ligne sans retour à la ligne") {
    const FichierTemporaire fichier{"recallio_releve_4.txt"};
    ecrire_brut(fichier.chemin, "a b\nc");
    CHECK(lire_lignes(fichier.chemin) == std::vector<std::string>{"a b", "c"});
}

TEST_CASE("additionne les entiers, quelle que soit leur disposition") {
    const FichierTemporaire fichier{"recallio_releve_5.txt"};
    ecrire_brut(fichier.chemin, "10 20\n\n  -5\n7");
    CHECK(somme(fichier.chemin) == 32);
}

TEST_CASE("donne zéro pour un fichier vide, et lance pour un fichier absent") {
    const FichierTemporaire fichier{"recallio_releve_6.txt"};
    ecrire_brut(fichier.chemin, "");
    CHECK(somme(fichier.chemin) == 0);
    CHECK_THROWS_AS(somme(introuvable), std::runtime_error);
}
