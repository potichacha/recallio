#pragma once

#include <cstddef>
#include <vector>

// Vrai si x est présent dans le vecteur trié.
bool contient(const std::vector<int>& valeurs, int x);

// Le nombre d'éléments égaux à x.
std::size_t occurrences(const std::vector<int>& valeurs, int x);

// Insère x à la place qui garde le vecteur trié.
void inserer(std::vector<int>& valeurs, int x);

// Le nombre d'éléments strictement inférieurs à x.
std::size_t nombre_inferieurs(const std::vector<int>& valeurs, int x);
