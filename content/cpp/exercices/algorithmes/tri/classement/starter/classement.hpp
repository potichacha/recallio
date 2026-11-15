#pragma once

#include <string>
#include <vector>

struct Joueur {
    std::string nom;
    int score;
};

// Trie du meilleur score au plus faible ; à score égal, par ordre alphabétique du nom.
void classer(std::vector<Joueur>& joueurs);

// Les noms des trois premiers du classement, ou de tous s'ils sont moins de trois.
std::vector<std::string> podium(std::vector<Joueur> joueurs);

// Le score médian : celui d'indice taille / 2 une fois les scores triés ; le vecteur n'est jamais vide.
int mediane(std::vector<int> scores);
