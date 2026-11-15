#include "ticket.hpp"

#include <format>

std::string prix(double euros) {
    return std::format("{:.2f} EUR", euros);
}

std::string numero(int n) {
    return std::format("#{:06}", n);
}

std::string ligne(std::string_view article, int quantite, double total) {
    return std::format("{:<12} x{:>3} {:>9.2f}", article, quantite, total);
}
