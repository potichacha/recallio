#pragma once

#include <filesystem>
#include <optional>
#include <string>
#include <vector>

// Écrit une chaîne par ligne, en remplaçant le contenu ; false si le fichier ne peut pas être ouvert.
bool ecrire_lignes(const std::filesystem::path& chemin, const std::vector<std::string>& lignes);

// Les lignes du fichier ; std::nullopt s'il ne peut pas être ouvert.
std::optional<std::vector<std::string>> lire_lignes(const std::filesystem::path& chemin);

// La somme des entiers du fichier ; lance std::runtime_error s'il ne peut pas être ouvert.
int somme(const std::filesystem::path& chemin);
