#include "mots.hpp"

std::map<std::string, int> compter(const std::vector<std::string>& mots) {
    std::map<std::string, int> compte;
    for (const auto& mot : mots) {
        ++compte[mot];
    }
    return compte;
}

int occurrences(const std::map<std::string, int>& compte, const std::string& mot) {
    const auto it = compte.find(mot);
    return it != compte.end() ? it->second : 0;
}

std::string plus_frequent(const std::map<std::string, int>& compte) {
    std::string meilleur;
    int maximum = 0;
    for (const auto& [mot, nombre] : compte) {
        if (nombre > maximum) {
            meilleur = mot;
            maximum = nombre;
        }
    }
    return meilleur;
}
