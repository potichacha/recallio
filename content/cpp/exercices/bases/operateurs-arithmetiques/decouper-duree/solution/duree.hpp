#pragma once

// Le nombre d'heures entières dans cette durée, donnée en secondes.
int heures(int secondes);

// Les minutes qui restent une fois les heures retirées, de 0 à 59.
int minutes(int secondes);

// Les secondes qui restent une fois les minutes retirées, de 0 à 59.
int secondes_restantes(int secondes);
