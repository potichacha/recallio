#pragma once

#include <cstddef>
#include <string>
#include <vector>

// Le carré de chaque nombre pair, dans l'ordre du vecteur.
std::vector<int> carres_des_pairs(const std::vector<int>& valeurs);

// Les n premiers mots de taille au moins égale à longueur ; tous s'il y en a moins de n.
std::vector<std::string> premiers_longs(const std::vector<std::string>& mots, std::size_t longueur, std::size_t n);

// Les n premiers multiples strictement positifs de k (k > 0).
std::vector<int> multiples(int k, std::size_t n);
