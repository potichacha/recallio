#include "notes.hpp"

double moyenne(const std::vector<int>& notes) {
    if (notes.empty()) {
        return 0.0;
    }
    int somme = 0;
    for (int note : notes) {
        somme += note;
    }
    return static_cast<double>(somme) / static_cast<double>(notes.size());
}

std::vector<int> garder_reussies(const std::vector<int>& notes) {
    std::vector<int> reussies;
    for (int note : notes) {
        if (note >= 10) {
            reussies.push_back(note);
        }
    }
    return reussies;
}
