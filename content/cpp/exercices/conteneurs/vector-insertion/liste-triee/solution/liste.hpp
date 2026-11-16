#pragma once

#include <cstddef>
#include <optional>
#include <vector>

// Insère la valeur dans un vecteur trié par ordre croissant, en le gardant trié.
void inserer_trie(std::vector<int>& valeurs, int valeur);

// Retire les nombres strictement négatifs, et renvoie le nombre d'éléments retirés.
std::size_t retirer_negatifs(std::vector<int>& valeurs);

// Retire le dernier élément et le renvoie ; std::nullopt si le vecteur est vide.
std::optional<int> retirer_dernier(std::vector<int>& valeurs);
