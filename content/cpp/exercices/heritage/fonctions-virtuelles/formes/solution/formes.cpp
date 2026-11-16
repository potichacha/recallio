#include "formes.hpp"

int Rectangle::aire() const {
    return largeur_ * hauteur_;
}

std::string Rectangle::nom() const {
    return "rectangle";
}

int Carre::aire() const {
    return cote_ * cote_;
}

std::string Carre::nom() const {
    return "carré";
}

int aire_totale(const std::vector<std::unique_ptr<Forme>>& formes) {
    int total = 0;
    for (const auto& forme : formes) {
        total += forme->aire();
    }
    return total;
}

std::string plus_grande(const std::vector<std::unique_ptr<Forme>>& formes) {
    const Forme* meilleure = nullptr;
    for (const auto& forme : formes) {
        if (meilleure == nullptr || forme->aire() > meilleure->aire()) {
            meilleure = forme.get();
        }
    }
    if (meilleure == nullptr) {
        return "";
    }
    return meilleure->nom();
}
