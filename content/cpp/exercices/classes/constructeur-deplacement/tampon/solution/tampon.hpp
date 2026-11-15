#pragma once

#include <cstddef>

class Tampon {
public:
    // Un tampon de cette taille, rempli de zéros.
    explicit Tampon(std::size_t taille) : taille_{taille}, donnees_{taille > 0 ? new int[taille]{} : nullptr} {}

    ~Tampon() { delete[] donnees_; }

    // À écrire dans tampon.cpp.
    Tampon(const Tampon& autre);
    Tampon& operator=(const Tampon& autre);
    Tampon(Tampon&& autre) noexcept;
    Tampon& operator=(Tampon&& autre) noexcept;

    std::size_t taille() const { return taille_; }
    bool vide() const { return donnees_ == nullptr; }
    int lire(std::size_t indice) const { return donnees_[indice]; }
    void ecrire(std::size_t indice, int valeur) { donnees_[indice] = valeur; }

private:
    std::size_t taille_;
    int* donnees_;
};
