#include "multiples.hpp"

int somme_des_multiples(int limite) {
    int somme = 0;
    for (int i = 1; i < limite; ++i) {
        if (i % 3 == 0 || i % 5 == 0) {
            somme += i;
        }
    }
    return somme;
}
