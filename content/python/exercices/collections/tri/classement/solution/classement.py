"""Classe des élèves d'après leur note."""


def classer(eleves):
    """Renvoie les noms, de la meilleure note à la moins bonne ; à note égale, dans l'ordre alphabétique.

    eleves est une liste de tuples (nom, note), qui n'est pas modifiée.
    """
    tries = sorted(eleves, key=lambda eleve: (-eleve[1], eleve[0]))
    noms = []
    for nom, _ in tries:
        noms.append(nom)
    return noms


def podium(eleves):
    """Renvoie les trois premiers noms du classement, ou moins s'il y a moins de trois élèves."""
    return classer(eleves)[:3]
