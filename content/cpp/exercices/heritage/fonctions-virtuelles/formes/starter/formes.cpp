#include "formes.hpp"

int Rectangle::aire() const {
    // À toi : la largeur multipliée par la hauteur.
    return 0;
}

std::string Rectangle::nom() const {
    // À toi : "rectangle".
    return "";
}

int Carre::aire() const {
    // À toi : le côté au carré.
    return 0;
}

std::string Carre::nom() const {
    // À toi : "carré".
    return "";
}

int aire_totale(const std::vector<std::unique_ptr<Forme>>& formes) {
    // À toi : la somme des aires.
    return 0;
}

std::string plus_grande(const std::vector<std::unique_ptr<Forme>>& formes) {
    // À toi : le nom de la forme la plus grande.
    return "";
}
