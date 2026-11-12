#pragma once

// Vrai si l'année est bissextile : divisible par 4 mais pas par 100, ou divisible par 400.
bool est_bissextile(int annee);

// Le nombre de jours du mois (de 1 à 12) de cette année ; 0 si le mois n'existe pas.
int jours_dans_mois(int mois, int annee);
