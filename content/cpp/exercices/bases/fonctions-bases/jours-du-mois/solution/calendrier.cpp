#include "calendrier.hpp"

bool est_bissextile(int annee) {
    return (annee % 4 == 0 && annee % 100 != 0) || annee % 400 == 0;
}

int jours_dans_mois(int mois, int annee) {
    if (mois < 1 || mois > 12) {
        return 0;
    }
    switch (mois) {
        case 4:
        case 6:
        case 9:
        case 11:
            return 30;
        case 2:
            return est_bissextile(annee) ? 29 : 28;
        default:
            return 31;
    }
}
