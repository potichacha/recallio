#pragma once

#include <cstddef>
#include <optional>
#include <vector>

// L'indice de la première occurrence de la note ; std::nullopt si elle est absente.
std::optional<std::size_t> position(const std::vector<int>& notes, int note);

// Le nombre de notes supérieures ou égales à 10.
std::size_t nombre_admis(const std::vector<int>& notes);

// Vrai si toutes les notes sont comprises entre 0 et 20 ; un vecteur vide est valide.
bool tous_valides(const std::vector<int>& notes);

// Vrai si au moins une note est strictement inférieure à 10.
bool contient_echec(const std::vector<int>& notes);
