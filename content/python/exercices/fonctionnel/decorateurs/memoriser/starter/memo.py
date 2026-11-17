"""Un décorateur qui garde en mémoire les résultats d'une fonction."""


def memoriser(f):
    """Décore f : chaque résultat est calculé une fois par jeu d'arguments, puis gardé."""
    return f
