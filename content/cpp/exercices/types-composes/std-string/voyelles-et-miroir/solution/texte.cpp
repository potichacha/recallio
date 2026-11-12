#include "texte.hpp"

int compter_voyelles(const std::string& texte) {
    const std::string voyelles = "aeiouyAEIOUY";
    int nombre = 0;
    for (char caractere : texte) {
        if (voyelles.find(caractere) != std::string::npos) {
            ++nombre;
        }
    }
    return nombre;
}

std::string inverser(const std::string& texte) {
    std::string resultat;
    for (std::size_t i = texte.size(); i > 0; --i) {
        resultat += texte[i - 1];
    }
    return resultat;
}
