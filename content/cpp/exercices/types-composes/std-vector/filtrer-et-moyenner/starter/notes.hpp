#pragma once

#include <vector>

// La moyenne des notes ; 0.0 pour un relevé vide.
double moyenne(const std::vector<int>& notes);

// Les notes qui valent au moins 10, dans le même ordre.
std::vector<int> garder_reussies(const std::vector<int>& notes);
