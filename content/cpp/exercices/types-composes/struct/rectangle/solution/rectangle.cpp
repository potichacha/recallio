#include "rectangle.hpp"

int aire(const Rectangle& r) {
    return r.largeur * r.hauteur;
}

int perimetre(const Rectangle& r) {
    return 2 * (r.largeur + r.hauteur);
}

Rectangle agrandir(const Rectangle& r, int facteur) {
    return {r.largeur * facteur, r.hauteur * facteur};
}
