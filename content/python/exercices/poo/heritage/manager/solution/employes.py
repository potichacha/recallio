"""Des employés et leurs managers."""


class Employe:
    """Un employé payé chaque mois, d'après son salaire annuel."""

    def __init__(self, nom, salaire_annuel):
        self.nom = nom
        self.salaire_annuel = salaire_annuel

    def paie_mensuelle(self):
        return self.salaire_annuel / 12

    def poste(self):
        return "employé"

    def presentation(self):
        return f"{self.nom}, {self.poste()}"


class Manager(Employe):
    """Un employé qui encadre une équipe, et touche une prime chaque mois."""

    def __init__(self, nom, salaire_annuel, prime_mensuelle):
        super().__init__(nom, salaire_annuel)
        self.prime_mensuelle = prime_mensuelle
        self.equipe = []

    def ajouter(self, employe):
        self.equipe.append(employe)

    def paie_mensuelle(self):
        return super().paie_mensuelle() + self.prime_mensuelle

    def poste(self):
        return f"manager ({len(self.equipe)})"
