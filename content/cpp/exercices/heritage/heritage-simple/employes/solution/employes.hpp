#pragma once

#include <string>

class Employe {
public:
    Employe(std::string nom, int salaire);

    const std::string& nom() const;
    int salaire() const;

private:
    std::string nom_;
    int salaire_;
};

class Manager : public Employe {
public:
    Manager(std::string nom, int salaire, int prime);

    int prime() const;

    // Le salaire plus la prime.
    int remuneration() const;

private:
    int prime_;
};
