#include "tri.hpp"

#include <algorithm>

bool contient(const std::vector<int>& valeurs, int x) {
    return std::binary_search(valeurs.begin(), valeurs.end(), x);
}

std::size_t occurrences(const std::vector<int>& valeurs, int x) {
    const auto debut = std::lower_bound(valeurs.begin(), valeurs.end(), x);
    const auto fin = std::upper_bound(valeurs.begin(), valeurs.end(), x);
    return static_cast<std::size_t>(fin - debut);
}

void inserer(std::vector<int>& valeurs, int x) {
    valeurs.insert(std::lower_bound(valeurs.begin(), valeurs.end(), x), x);
}

std::size_t nombre_inferieurs(const std::vector<int>& valeurs, int x) {
    return static_cast<std::size_t>(std::lower_bound(valeurs.begin(), valeurs.end(), x) - valeurs.begin());
}
