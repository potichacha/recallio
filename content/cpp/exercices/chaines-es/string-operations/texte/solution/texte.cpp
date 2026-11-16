#include "texte.hpp"

std::string extension(const std::string& nom) {
    const auto point = nom.rfind('.');
    if (point == std::string::npos) {
        return "";
    }
    return nom.substr(point + 1);
}

std::string remplacer_tout(std::string texte, const std::string& ancien, const std::string& nouveau) {
    std::size_t position = texte.find(ancien);
    while (position != std::string::npos) {
        texte.replace(position, ancien.size(), nouveau);
        position = texte.find(ancien, position + nouveau.size());
    }
    return texte;
}

std::vector<std::string> decouper(const std::string& texte, char separateur) {
    std::vector<std::string> morceaux;
    std::size_t debut = 0;
    std::size_t position = texte.find(separateur);
    while (position != std::string::npos) {
        morceaux.push_back(texte.substr(debut, position - debut));
        debut = position + 1;
        position = texte.find(separateur, debut);
    }
    morceaux.push_back(texte.substr(debut));
    return morceaux;
}
