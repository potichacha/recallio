#pragma once

#include <vector>

// La température en kelvins ; lance std::invalid_argument sous le zéro absolu (-273.15 °C).
double convertir(double celsius);

// La moyenne des températures ; lance std::invalid_argument pour un vecteur vide.
double moyenne(const std::vector<double>& celsius);

// convertir(celsius), ou defaut si la conversion échoue ; ne laisse sortir aucune exception.
double convertir_ou(double celsius, double defaut);
