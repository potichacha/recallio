#include "tampon.hpp"

#include <algorithm>

Tampon::Tampon(const Tampon& autre)
    : taille_{autre.taille_}, donnees_{autre.taille_ > 0 ? new int[autre.taille_] : nullptr} {
    std::copy(autre.donnees_, autre.donnees_ + taille_, donnees_);
}

Tampon& Tampon::operator=(const Tampon& autre) {
    if (this != &autre) {
        int* copie = autre.taille_ > 0 ? new int[autre.taille_] : nullptr;
        std::copy(autre.donnees_, autre.donnees_ + autre.taille_, copie);
        delete[] donnees_;
        donnees_ = copie;
        taille_ = autre.taille_;
    }
    return *this;
}

Tampon::Tampon(Tampon&& autre) noexcept : taille_{autre.taille_}, donnees_{autre.donnees_} {
    autre.taille_ = 0;
    autre.donnees_ = nullptr;
}

Tampon& Tampon::operator=(Tampon&& autre) noexcept {
    if (this != &autre) {
        delete[] donnees_;
        taille_ = autre.taille_;
        donnees_ = autre.donnees_;
        autre.taille_ = 0;
        autre.donnees_ = nullptr;
    }
    return *this;
}
