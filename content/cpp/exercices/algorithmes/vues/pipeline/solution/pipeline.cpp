#include "pipeline.hpp"

#include <ranges>

std::vector<int> carres_des_pairs(const std::vector<int>& valeurs) {
    return valeurs
        | std::views::filter([](int x) { return x % 2 == 0; })
        | std::views::transform([](int x) { return x * x; })
        | std::ranges::to<std::vector>();
}

std::vector<std::string> premiers_longs(const std::vector<std::string>& mots, std::size_t longueur, std::size_t n) {
    return mots
        | std::views::filter([longueur](const std::string& mot) { return mot.size() >= longueur; })
        | std::views::take(n)
        | std::ranges::to<std::vector>();
}

std::vector<int> multiples(int k, std::size_t n) {
    return std::views::iota(1)
        | std::views::transform([k](int i) { return i * k; })
        | std::views::take(n)
        | std::ranges::to<std::vector>();
}
