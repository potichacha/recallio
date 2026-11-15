#pragma once

#include <string>
#include <vector>

// Ce qui suit le dernier point du nom ; une chaîne vide s'il n'y a pas de point.
std::string extension(const std::string& nom);

// Le texte où chaque occurrence de ancien (jamais vide) est remplacée par nouveau.
std::string remplacer_tout(std::string texte, const std::string& ancien, const std::string& nouveau);

// Les morceaux du texte, découpé autour du séparateur.
std::vector<std::string> decouper(const std::string& texte, char separateur);
