"""Une commande en ligne et ses lignes."""

from dataclasses import dataclass, field


@dataclass(frozen=True)
class Ligne:
    """Un produit commandé, son prix unitaire et sa quantité."""

    produit: str
    prix_unitaire: float
    quantite: int = 1

    def __post_init__(self):
        if self.quantite < 1:
            raise ValueError(f"quantité invalide : {self.quantite}")
        if self.prix_unitaire < 0:
            raise ValueError(f"prix négatif : {self.prix_unitaire}")

    @property
    def total(self):
        return self.prix_unitaire * self.quantite


@dataclass
class Commande:
    """La commande d'un client."""

    client: str
    lignes: list[Ligne] = field(default_factory=list)

    def ajouter(self, ligne):
        self.lignes.append(ligne)

    def total(self):
        return sum(ligne.total for ligne in self.lignes)
