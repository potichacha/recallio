#include "outils.hpp"

#include <algorithm>

std::size_t compter_pairs(const std::vector<int>& valeurs) {
    const auto pairs = std::count_if(valeurs.begin(), valeurs.end(), [](int x) { return x % 2 == 0; });
    return static_cast<std::size_t>(pairs);
}

void trier_par_longueur(std::vector<std::string>& mots) {
    std::sort(mots.begin(), mots.end(), [](const std::string& a, const std::string& b) {
        if (a.size() != b.size()) {
            return a.size() < b.size();
        }
        return a < b;
    });
}

void doubler(std::vector<int>& valeurs) {
    std::for_each(valeurs.begin(), valeurs.end(), [](int& x) { x *= 2; });
}
