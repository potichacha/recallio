#include "ticket.hpp"

#include <format>

std::string prix(double euros) {
    // À toi : deux chiffres après la virgule.
    return std::format("{}", euros);
}

std::string numero(int n) {
    // À toi : six chiffres, complétés par des zéros.
    return std::format("{}", n);
}

std::string ligne(std::string_view article, int quantite, double total) {
    // À toi : trois champs alignés.
    return std::format("{} {} {}", article, quantite, total);
}
