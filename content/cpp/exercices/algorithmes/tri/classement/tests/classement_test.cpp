#include "classement.hpp"
#include "doctest.h"

#include <string>
#include <vector>

namespace {

std::vector<std::string> noms(const std::vector<Joueur>& joueurs) {
    std::vector<std::string> resultat;
    for (const auto& joueur : joueurs) {
        resultat.push_back(joueur.nom);
    }
    return resultat;
}

}  // namespace

TEST_CASE("classe du meilleur score au plus faible") {
    std::vector<Joueur> joueurs{{"Ada", 40}, {"Linus", 90}, {"Grace", 70}};
    classer(joueurs);
    CHECK(noms(joueurs) == std::vector<std::string>{"Linus", "Grace", "Ada"});
}

TEST_CASE("départage un score égal par le nom") {
    std::vector<Joueur> joueurs{{"Zoé", 50}, {"Ali", 50}, {"Léa", 80}, {"Bob", 50}};
    classer(joueurs);
    CHECK(noms(joueurs) == std::vector<std::string>{"Léa", "Ali", "Bob", "Zoé"});
}

TEST_CASE("donne les trois premiers du classement") {
    const std::vector<Joueur> joueurs{{"Ada", 40}, {"Linus", 90}, {"Grace", 70}, {"Alan", 85}};
    CHECK(podium(joueurs) == std::vector<std::string>{"Linus", "Alan", "Grace"});
}

TEST_CASE("donne tous les joueurs s'ils sont moins de trois") {
    CHECK(podium({{"Ada", 40}, {"Linus", 90}}) == std::vector<std::string>{"Linus", "Ada"});
    CHECK(podium({}).empty());
}

TEST_CASE("ne modifie pas le vecteur reçu par le podium") {
    const std::vector<Joueur> joueurs{{"Ada", 40}, {"Linus", 90}};
    podium(joueurs);
    CHECK(noms(joueurs) == std::vector<std::string>{"Ada", "Linus"});
}

TEST_CASE("trouve la médiane d'un nombre impair de scores") {
    CHECK(mediane({50, 10, 90, 30, 70}) == 50);
    CHECK(mediane({7}) == 7);
}

TEST_CASE("prend le plus grand des deux du milieu pour un nombre pair") {
    CHECK(mediane({40, 10, 30, 20}) == 30);
}
