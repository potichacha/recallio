#pragma once

// La valeur, avec le bit de ce rang mis à 1.
unsigned activer(unsigned valeur, int rang);

// La valeur, avec le bit de ce rang mis à 0.
unsigned desactiver(unsigned valeur, int rang);

// Vrai si le bit de ce rang vaut 1.
bool est_actif(unsigned valeur, int rang);

// Le nombre de bits à 1.
int compter(unsigned valeur);
