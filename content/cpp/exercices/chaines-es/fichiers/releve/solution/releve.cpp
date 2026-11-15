#include "releve.hpp"

#include <fstream>
#include <stdexcept>

bool ecrire_lignes(const std::filesystem::path& chemin, const std::vector<std::string>& lignes) {
    std::ofstream sortie{chemin};
    if (!sortie) {
        return false;
    }
    for (const auto& ligne : lignes) {
        sortie << ligne << '\n';
    }
    return true;
}

std::optional<std::vector<std::string>> lire_lignes(const std::filesystem::path& chemin) {
    std::ifstream entree{chemin};
    if (!entree) {
        return std::nullopt;
    }
    std::vector<std::string> lignes;
    std::string ligne;
    while (std::getline(entree, ligne)) {
        lignes.push_back(ligne);
    }
    return lignes;
}

int somme(const std::filesystem::path& chemin) {
    std::ifstream entree{chemin};
    if (!entree) {
        throw std::runtime_error{"fichier illisible"};
    }
    int total = 0;
    int valeur = 0;
    while (entree >> valeur) {
        total += valeur;
    }
    return total;
}
