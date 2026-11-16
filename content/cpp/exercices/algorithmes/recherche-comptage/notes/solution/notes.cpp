#include "notes.hpp"

#include <algorithm>

std::optional<std::size_t> position(const std::vector<int>& notes, int note) {
    const auto it = std::find(notes.begin(), notes.end(), note);
    if (it == notes.end()) {
        return std::nullopt;
    }
    return static_cast<std::size_t>(it - notes.begin());
}

std::size_t nombre_admis(const std::vector<int>& notes) {
    const auto admis = std::count_if(notes.begin(), notes.end(), [](int n) { return n >= 10; });
    return static_cast<std::size_t>(admis);
}

bool tous_valides(const std::vector<int>& notes) {
    return std::all_of(notes.begin(), notes.end(), [](int n) { return n >= 0 && n <= 20; });
}

bool contient_echec(const std::vector<int>& notes) {
    return std::any_of(notes.begin(), notes.end(), [](int n) { return n < 10; });
}
