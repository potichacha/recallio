#include "fraction.hpp"

#include <numeric>

Fraction::Fraction(int numerateur, int denominateur) : numerateur_{numerateur}, denominateur_{denominateur} {
    const int diviseur = std::gcd(numerateur_, denominateur_);
    if (diviseur != 0) {
        numerateur_ /= diviseur;
        denominateur_ /= diviseur;
    }
    if (denominateur_ < 0) {
        numerateur_ = -numerateur_;
        denominateur_ = -denominateur_;
    }
}

Fraction& Fraction::operator+=(const Fraction& autre) {
    // À toi : calcule la somme, et affecte-la à *this.
    return *this;
}

Fraction operator+(Fraction a, const Fraction& b) {
    // À toi : sers-toi de +=.
    return a;
}

Fraction operator*(const Fraction& a, const Fraction& b) {
    // À toi : le produit des numérateurs sur le produit des dénominateurs.
    return a;
}

bool operator==(const Fraction& a, const Fraction& b) {
    // À toi : compare les numérateurs et les dénominateurs.
    return false;
}
