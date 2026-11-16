#include "pipeline.hpp"

#include <ranges>

std::vector<int> carres_des_pairs(const std::vector<int>& valeurs) {
    // À toi : filter, puis transform, puis std::ranges::to<std::vector>().
    return valeurs;
}

std::vector<std::string> premiers_longs(const std::vector<std::string>& mots, std::size_t longueur, std::size_t n) {
    // À toi : filter, puis take.
    return mots;
}

std::vector<int> multiples(int k, std::size_t n) {
    // À toi : std::views::iota(1), transform, take.
    return {};
}
