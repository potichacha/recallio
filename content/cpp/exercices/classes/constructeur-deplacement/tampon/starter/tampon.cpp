#include "tampon.hpp"

// À toi : une copie profonde. Pour l'instant, la copie est un tampon vide.
Tampon::Tampon(const Tampon& autre) : taille_{0}, donnees_{nullptr} {
}

// À toi : une copie profonde, y compris pour a = a.
Tampon& Tampon::operator=(const Tampon& autre) {
    return *this;
}

// À toi : prends le tableau de la source, et laisse-la vide.
Tampon::Tampon(Tampon&& autre) noexcept : taille_{0}, donnees_{nullptr} {
}

// À toi : libère l'ancien tableau, prends celui de la source, et laisse-la vide.
Tampon& Tampon::operator=(Tampon&& autre) noexcept {
    return *this;
}
