"""Une commande en ligne et ses lignes."""


class Ligne:
    """Un produit commandé, son prix unitaire et sa quantité."""

    def __init__(self, produit, prix_unitaire, quantite=1):
        self.produit = produit
        self.prix_unitaire = prix_unitaire
        self.quantite = quantite

    @property
    def total(self):
        return 0


class Commande:
    """La commande d'un client."""

    def __init__(self, client, lignes=[]):
        self.client = client
        self.lignes = lignes

    def ajouter(self, ligne):
        self.lignes.append(ligne)

    def total(self):
        return 0
