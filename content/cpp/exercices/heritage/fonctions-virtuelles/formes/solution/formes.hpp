#pragma once

#include <memory>
#include <string>
#include <vector>

class Forme {
public:
    virtual ~Forme() = default;
    virtual int aire() const = 0;
    virtual std::string nom() const = 0;
};

class Rectangle : public Forme {
public:
    Rectangle(int largeur, int hauteur) : largeur_{largeur}, hauteur_{hauteur} {}
    int aire() const override;
    std::string nom() const override;

private:
    int largeur_;
    int hauteur_;
};

class Carre : public Forme {
public:
    explicit Carre(int cote) : cote_{cote} {}
    int aire() const override;
    std::string nom() const override;

private:
    int cote_;
};

// La somme des aires de toutes les formes.
int aire_totale(const std::vector<std::unique_ptr<Forme>>& formes);

// Le nom de la forme dont l'aire est la plus grande ; une chaîne vide si le vecteur est vide.
std::string plus_grande(const std::vector<std::unique_ptr<Forme>>& formes);
