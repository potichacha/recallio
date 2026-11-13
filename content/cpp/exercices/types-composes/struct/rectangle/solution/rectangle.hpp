#pragma once

struct Rectangle {
    int largeur;
    int hauteur;
};

// La surface du rectangle.
int aire(const Rectangle& r);

// Le tour du rectangle.
int perimetre(const Rectangle& r);

// Un nouveau rectangle, dont les deux dimensions sont multipliées par facteur.
Rectangle agrandir(const Rectangle& r, int facteur);
