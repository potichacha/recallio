#include "temperature.hpp"

#include <numeric>
#include <stdexcept>

namespace {

constexpr double zero_absolu = -273.15;

}  // namespace

double convertir(double celsius) {
    if (celsius < zero_absolu) {
        throw std::invalid_argument{"température sous le zéro absolu"};
    }
    return celsius - zero_absolu;
}

double moyenne(const std::vector<double>& celsius) {
    if (celsius.empty()) {
        throw std::invalid_argument{"aucune température"};
    }
    const double somme = std::accumulate(celsius.begin(), celsius.end(), 0.0);
    return somme / static_cast<double>(celsius.size());
}

double convertir_ou(double celsius, double defaut) {
    try {
        return convertir(celsius);
    } catch (const std::invalid_argument&) {
        return defaut;
    }
}
