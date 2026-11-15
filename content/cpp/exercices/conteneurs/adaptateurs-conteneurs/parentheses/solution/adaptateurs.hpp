#pragma once

#include <cstddef>
#include <string_view>
#include <vector>

// Vrai si (), [] et {} sont correctement appariés et imbriqués ; les autres caractères sont ignorés.
bool bien_parenthese(std::string_view texte);

// Les k plus grandes valeurs, de la plus grande à la plus petite ; toutes s'il y en a moins de k.
std::vector<int> plus_grands(const std::vector<int>& valeurs, std::size_t k);
