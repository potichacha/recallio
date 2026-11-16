#pragma once

// Le plus petit des deux.
template <typename T>
T minimum(T a, T b) {
    return b < a ? b : a;
}

// Le plus grand des deux.
template <typename T>
T maximum(T a, T b) {
    return a < b ? b : a;
}

// La valeur ramenée dans l'intervalle [bas, haut].
template <typename T>
T borner(T valeur, T bas, T haut) {
    if (valeur < bas) {
        return bas;
    }
    if (haut < valeur) {
        return haut;
    }
    return valeur;
}
