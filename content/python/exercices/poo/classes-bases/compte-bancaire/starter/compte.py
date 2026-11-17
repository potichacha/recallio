"""Un compte bancaire : un titulaire, un solde et l'historique de ses opérations."""


class Compte:
    """Le compte d'un titulaire."""

    def __init__(self, titulaire, solde=0):
        self.titulaire = titulaire

    def deposer(self, montant):
        """Ajoute un montant strictement positif au solde."""
        raise NotImplementedError

    def retirer(self, montant):
        """Retire un montant strictement positif, qui ne dépasse pas le solde."""
        raise NotImplementedError
