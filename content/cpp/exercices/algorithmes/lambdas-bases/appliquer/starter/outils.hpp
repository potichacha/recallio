#pragma once

#include <cstddef>
#include <string>
#include <vector>

// Le nombre d'entiers pairs.
std::size_t compter_pairs(const std::vector<int>& valeurs);

// Trie du plus court au plus long ; à longueur égale, par ordre alphabétique.
void trier_par_longueur(std::vector<std::string>& mots);

// Multiplie chaque élément par deux, en place.
void doubler(std::vector<int>& valeurs);
