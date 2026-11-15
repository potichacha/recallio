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
    *this = Fraction{numerateur_ * autre.denominateur_ + autre.numerateur_ * denominateur_,
                     denominateur_ * autre.denominateur_};
    return *this;
}

Fraction operator+(Fraction a, const Fraction& b) {
    a += b;
    return a;
}

Fraction operator*(const Fraction& a, const Fraction& b) {
    return Fraction{a.numerateur() * b.numerateur(), a.denominateur() * b.denominateur()};
}

bool operator==(const Fraction& a, const Fraction& b) {
    return a.numerateur() == b.numerateur() && a.denominateur() == b.denominateur();
}
