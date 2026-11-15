#include "temperature.hpp"

#include <numeric>
#include <stdexcept>

double convertir(double celsius) {
    // À toi : lance std::invalid_argument sous le zéro absolu.
    return celsius + 273.15;
}

double moyenne(const std::vector<double>& celsius) {
    // À toi : lance std::invalid_argument pour un vecteur vide, sinon calcule la moyenne.
    return 0.0;
}

double convertir_ou(double celsius, double defaut) {
    // À toi : attrape l'exception de convertir, et renvoie defaut.
    return convertir(celsius);
}
