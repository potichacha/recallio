#include "bits.hpp"

unsigned activer(unsigned valeur, int rang) {
    return valeur | (1u << rang);
}

unsigned desactiver(unsigned valeur, int rang) {
    return valeur & ~(1u << rang);
}

bool est_actif(unsigned valeur, int rang) {
    return (valeur & (1u << rang)) != 0;
}

int compter(unsigned valeur) {
    int nombre = 0;
    while (valeur != 0) {
        nombre += static_cast<int>(valeur & 1u);
        valeur >>= 1;
    }
    return nombre;
}
