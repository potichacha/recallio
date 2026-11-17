"""Un compte bancaire : un titulaire, un solde et l'historique de ses opérations."""


class Compte:
    """Le compte d'un titulaire."""

    def __init__(self, titulaire, solde=0):
        self.titulaire = titulaire
        self.solde = solde
        self.operations = []

    def deposer(self, montant):
        """Ajoute un montant strictement positif au solde."""
        self._verifier_montant(montant)
        self.solde += montant
        self.operations.append(("dépôt", montant))

    def retirer(self, montant):
        """Retire un montant strictement positif, qui ne dépasse pas le solde."""
        self._verifier_montant(montant)
        if montant > self.solde:
            raise ValueError("solde insuffisant")
        self.solde -= montant
        self.operations.append(("retrait", montant))

    def _verifier_montant(self, montant):
        if montant <= 0:
            raise ValueError("le montant doit être strictement positif")
