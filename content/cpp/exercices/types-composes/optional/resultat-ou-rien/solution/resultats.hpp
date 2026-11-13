#pragma once

#include <optional>
#include <vector>

// La division entière de a par b, ou rien si b vaut zéro.
std::optional<int> quotient(int a, int b);

// Le premier nombre pair du vecteur, ou rien s'il n'y en a aucun.
std::optional<int> premier_pair(const std::vector<int>& valeurs);
