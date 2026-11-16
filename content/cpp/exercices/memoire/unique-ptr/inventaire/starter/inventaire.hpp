#pragma once

#include <cstddef>
#include <memory>
#include <string>
#include <vector>

struct Article {
    std::string nom;
    int prix;
};

class Inventaire {
public:
    // Prend possession de l'article, et le range à la fin ; un pointeur nul est ignoré.
    void ajouter(std::unique_ptr<Article> article);

    // Le nombre d'articles.
    std::size_t nombre() const;

    // La somme des prix.
    int valeur_totale() const;

    // Retire le dernier article et le rend à l'appelant ; un pointeur nul si l'inventaire est vide.
    std::unique_ptr<Article> retirer_dernier();

private:
    std::vector<std::unique_ptr<Article>> articles_;
};
