#include "ordre.hpp"

void echanger(int& a, int& b) {
    int ancien = a;
    a = b;
    b = ancien;
}

void ordonner(int& a, int& b) {
    if (a > b) {
        echanger(a, b);
    }
}
