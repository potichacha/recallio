#include "inventaire.hpp"

#include <utility>

void Inventaire::ajouter(std::unique_ptr<Article> article) {
    // À toi : range l'article dans le vecteur, s'il n'est pas nul.
}

std::size_t Inventaire::nombre() const {
    return articles_.size();
}

int Inventaire::valeur_totale() const {
    // À toi : la somme des prix des articles.
    return 0;
}

std::unique_ptr<Article> Inventaire::retirer_dernier() {
    // À toi : retire le dernier article, et rends-le à l'appelant.
    return nullptr;
}
