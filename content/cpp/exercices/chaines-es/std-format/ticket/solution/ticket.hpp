#pragma once

#include <string>
#include <string_view>

// Le montant avec deux décimales, suivi de " EUR" : "12.50 EUR".
std::string prix(double euros);

// Le numéro sur six chiffres, précédé de # : "#000042".
std::string numero(int n);

// L'article à gauche sur 12, "x" et la quantité à droite sur 3, le total à droite sur 9 avec deux décimales.
std::string ligne(std::string_view article, int quantite, double total);
