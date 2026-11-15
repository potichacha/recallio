#include "recursif.hpp"

int puissance(int base, int exposant) {
    if (exposant == 0) {
        return 1;
    }
    return base * puissance(base, exposant - 1);
}

int somme_chiffres(int n) {
    if (n < 10) {
        return n;
    }
    return n % 10 + somme_chiffres(n / 10);
}

int pgcd(int a, int b) {
    if (b == 0) {
        return a;
    }
    return pgcd(b, a % b);
}
