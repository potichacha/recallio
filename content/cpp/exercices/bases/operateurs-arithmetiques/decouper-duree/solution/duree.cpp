#include "duree.hpp"

int heures(int secondes) {
    return secondes / 3600;
}

int minutes(int secondes) {
    return secondes % 3600 / 60;
}

int secondes_restantes(int secondes) {
    return secondes % 60;
}
