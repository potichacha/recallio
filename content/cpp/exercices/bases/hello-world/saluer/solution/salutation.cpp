#include "salutation.hpp"

std::string saluer(std::string prenom) {
    if (prenom.empty()) {
        return "Bonjour !";
    }
    return "Bonjour, " + prenom + " !";
}
