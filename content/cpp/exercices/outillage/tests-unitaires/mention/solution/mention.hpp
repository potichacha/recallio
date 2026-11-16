#pragma once

#include <string>

// La mention d'une note sur 20 ; lance std::invalid_argument hors de l'intervalle [0, 20].
std::string mention(double note);
