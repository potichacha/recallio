#include "classement.hpp"

#include <algorithm>

void classer(std::vector<Joueur>& joueurs) {
    std::sort(joueurs.begin(), joueurs.end(), [](const Joueur& a, const Joueur& b) {
        if (a.score != b.score) {
            return a.score > b.score;
        }
        return a.nom < b.nom;
    });
}

std::vector<std::string> podium(std::vector<Joueur> joueurs) {
    classer(joueurs);
    std::vector<std::string> noms;
    const std::size_t places = std::min<std::size_t>(3, joueurs.size());
    for (std::size_t i = 0; i < places; ++i) {
        noms.push_back(joueurs[i].nom);
    }
    return noms;
}

int mediane(std::vector<int> scores) {
    const auto milieu = scores.begin() + static_cast<std::ptrdiff_t>(scores.size() / 2);
    std::nth_element(scores.begin(), milieu, scores.end());
    return *milieu;
}
