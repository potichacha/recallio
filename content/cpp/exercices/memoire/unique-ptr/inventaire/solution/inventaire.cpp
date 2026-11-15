#include "inventaire.hpp"

#include <utility>

void Inventaire::ajouter(std::unique_ptr<Article> article) {
    if (article) {
        articles_.push_back(std::move(article));
    }
}

std::size_t Inventaire::nombre() const {
    return articles_.size();
}

int Inventaire::valeur_totale() const {
    int total = 0;
    for (const auto& article : articles_) {
        total += article->prix;
    }
    return total;
}

std::unique_ptr<Article> Inventaire::retirer_dernier() {
    if (articles_.empty()) {
        return nullptr;
    }
    std::unique_ptr<Article> dernier = std::move(articles_.back());
    articles_.pop_back();
    return dernier;
}
