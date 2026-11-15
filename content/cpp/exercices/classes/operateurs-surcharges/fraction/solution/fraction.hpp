#pragma once

class Fraction {
public:
    // Construit la fraction réduite numerateur / denominateur ; le dénominateur n'est jamais nul.
    Fraction(int numerateur, int denominateur = 1);

    int numerateur() const { return numerateur_; }
    int denominateur() const { return denominateur_; }

    // Ajoute l'autre fraction à celle-ci, et renvoie *this.
    Fraction& operator+=(const Fraction& autre);

private:
    int numerateur_;
    int denominateur_;
};

// La somme des deux fractions.
Fraction operator+(Fraction a, const Fraction& b);

// Le produit des deux fractions.
Fraction operator*(const Fraction& a, const Fraction& b);

// Vrai si les deux fractions sont égales.
bool operator==(const Fraction& a, const Fraction& b);
