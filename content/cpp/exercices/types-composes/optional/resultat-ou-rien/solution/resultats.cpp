#include "resultats.hpp"

std::optional<int> quotient(int a, int b) {
    if (b == 0) {
        return std::nullopt;
    }
    return a / b;
}

std::optional<int> premier_pair(const std::vector<int>& valeurs) {
    for (int valeur : valeurs) {
        if (valeur % 2 == 0) {
            return valeur;
        }
    }
    return std::nullopt;
}
