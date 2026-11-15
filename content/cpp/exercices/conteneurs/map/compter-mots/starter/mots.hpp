#pragma once

#include <map>
#include <string>
#include <vector>

// Le nombre d'occurrences de chaque mot.
std::map<std::string, int> compter(const std::vector<std::string>& mots);

// Le nombre d'occurrences du mot, ou 0 s'il est absent ; n'ajoute rien à la map.
int occurrences(const std::map<std::string, int>& compte, const std::string& mot);

// Le mot le plus fréquent ; en cas d'égalité, le premier dans l'ordre alphabétique ; une chaîne vide si la map est vide.
std::string plus_frequent(const std::map<std::string, int>& compte);
