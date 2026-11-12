#pragma once

#include <string>

// Le nombre de voyelles (a, e, i, o, u, y), en minuscules comme en majuscules.
int compter_voyelles(const std::string& texte);

// Le texte lu de droite à gauche.
std::string inverser(const std::string& texte);
