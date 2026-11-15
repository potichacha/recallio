#include "notes.hpp"

#include <algorithm>

std::optional<std::size_t> position(const std::vector<int>& notes, int note) {
    // À toi : std::find, puis la distance au début.
    return std::nullopt;
}

std::size_t nombre_admis(const std::vector<int>& notes) {
    // À toi : std::count_if.
    return 0;
}

bool tous_valides(const std::vector<int>& notes) {
    // À toi : std::all_of.
    return false;
}

bool contient_echec(const std::vector<int>& notes) {
    // À toi : std::any_of.
    return false;
}
