#include "doctest.h"
#include "fraction.hpp"

TEST_CASE("compare deux fractions égales") {
    CHECK(Fraction{1, 2} == Fraction{1, 2});
    CHECK(Fraction{2, 4} == Fraction{1, 2});
}

TEST_CASE("distingue deux fractions différentes") {
    CHECK_FALSE(Fraction{1, 2} == Fraction{1, 3});
    CHECK_FALSE(Fraction{1, 2} == Fraction{2, 2});
}

TEST_CASE("ajoute une fraction à une autre avec +=") {
    Fraction f{1, 2};
    f += Fraction{1, 3};
    CHECK(f.numerateur() == 5);
    CHECK(f.denominateur() == 6);
}

TEST_CASE("renvoie l'objet lui-même depuis +=") {
    Fraction f{1, 4};
    (f += Fraction{1, 4}) += Fraction{1, 4};
    CHECK(f.numerateur() == 3);
    CHECK(f.denominateur() == 4);
}

TEST_CASE("additionne sans modifier les opérandes") {
    const Fraction a{1, 2};
    const Fraction b{1, 6};
    const Fraction somme = a + b;
    CHECK(somme.numerateur() == 2);
    CHECK(somme.denominateur() == 3);
    CHECK(a.numerateur() == 1);
    CHECK(a.denominateur() == 2);
}

TEST_CASE("multiplie deux fractions et réduit le résultat") {
    const Fraction produit = Fraction{2, 3} * Fraction{3, 4};
    CHECK(produit.numerateur() == 1);
    CHECK(produit.denominateur() == 2);
}

TEST_CASE("accepte un entier à gauche comme à droite") {
    const Fraction demi{1, 2};
    const Fraction somme = 2 + demi;
    CHECK(somme.numerateur() == 5);
    CHECK(somme.denominateur() == 2);
    const Fraction produit = demi * 4;
    CHECK(produit.numerateur() == 2);
    CHECK(produit.denominateur() == 1);
}
