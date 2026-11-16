#include "liste.hpp"

void inserer_trie(std::vector<int>& valeurs, int valeur) {
    auto position = valeurs.begin();
    while (position != valeurs.end() && *position < valeur) {
        ++position;
    }
    valeurs.insert(position, valeur);
}

std::size_t retirer_negatifs(std::vector<int>& valeurs) {
    std::size_t retires = 0;
    for (auto it = valeurs.begin(); it != valeurs.end();) {
        if (*it < 0) {
            it = valeurs.erase(it);
            ++retires;
        } else {
            ++it;
        }
    }
    return retires;
}

std::optional<int> retirer_dernier(std::vector<int>& valeurs) {
    if (valeurs.empty()) {
        return std::nullopt;
    }
    const int dernier = valeurs.back();
    valeurs.pop_back();
    return dernier;
}
