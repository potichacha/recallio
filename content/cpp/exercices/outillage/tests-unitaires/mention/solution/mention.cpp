#include "mention.hpp"

#include <stdexcept>

std::string mention(double note) {
    if (note < 0.0 || note > 20.0) {
        throw std::invalid_argument{"note hors de l'intervalle [0, 20]"};
    }
    if (note >= 16.0) {
        return "Très bien";
    }
    if (note >= 14.0) {
        return "Bien";
    }
    if (note >= 12.0) {
        return "Assez bien";
    }
    if (note >= 10.0) {
        return "Passable";
    }
    return "Refusé";
}
