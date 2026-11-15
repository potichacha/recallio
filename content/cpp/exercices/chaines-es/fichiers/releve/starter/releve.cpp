#include "releve.hpp"

#include <fstream>
#include <stdexcept>

bool ecrire_lignes(const std::filesystem::path& chemin, const std::vector<std::string>& lignes) {
    // À toi : ouvre un std::ofstream, teste l'ouverture, écris chaque ligne.
    return false;
}

std::optional<std::vector<std::string>> lire_lignes(const std::filesystem::path& chemin) {
    // À toi : ouvre un std::ifstream, teste l'ouverture, lis avec std::getline.
    return std::nullopt;
}

int somme(const std::filesystem::path& chemin) {
    // À toi : ouvre, lance std::runtime_error si l'ouverture échoue, additionne avec >>.
    return 0;
}
