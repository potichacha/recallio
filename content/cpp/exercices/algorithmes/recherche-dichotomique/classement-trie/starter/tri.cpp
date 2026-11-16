#include "tri.hpp"

#include <algorithm>

bool contient(const std::vector<int>& valeurs, int x) {
    // À toi : std::binary_search.
    return false;
}

std::size_t occurrences(const std::vector<int>& valeurs, int x) {
    // À toi : la différence entre std::upper_bound et std::lower_bound.
    return 0;
}

void inserer(std::vector<int>& valeurs, int x) {
    // À toi : insère à la position que donne std::lower_bound.
    valeurs.push_back(x);
}

std::size_t nombre_inferieurs(const std::vector<int>& valeurs, int x) {
    // À toi : la distance entre le début et std::lower_bound.
    return 0;
}
