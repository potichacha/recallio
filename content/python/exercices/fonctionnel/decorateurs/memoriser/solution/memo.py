"""Un décorateur qui garde en mémoire les résultats d'une fonction."""
import functools


def memoriser(f):
    """Décore f : chaque résultat est calculé une fois par jeu d'arguments, puis gardé."""
    resultats = {}

    @functools.wraps(f)
    def enveloppe(*args):
        if args not in resultats:
            resultats[args] = f(*args)
        return resultats[args]

    return enveloppe
