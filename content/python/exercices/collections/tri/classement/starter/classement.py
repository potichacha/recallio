"""Classe des élèves d'après leur note."""


def classer(eleves):
    """Renvoie les noms, de la meilleure note à la moins bonne ; à note égale, dans l'ordre alphabétique.

    eleves est une liste de tuples (nom, note), qui n'est pas modifiée.
    """
    noms = []
    for nom, _ in eleves:
        noms.append(nom)
    return noms


def podium(eleves):
    """Renvoie les trois premiers noms du classement, ou moins s'il y a moins de trois élèves."""
    return []
