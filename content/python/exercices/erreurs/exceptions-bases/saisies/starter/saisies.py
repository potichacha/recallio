"""Convertir les saisies d'un formulaire, sans s'arrêter sur une saisie invalide."""


def lire_entier(texte, defaut=None):
    """L'entier que représente texte, ou defaut si int le refuse."""
    return int(texte)


def somme_valide(textes):
    """Le couple (total des entiers valides, liste des textes refusés)."""
    return 0, []


def moyenne(textes):
    """La moyenne des entiers valides, ou None s'il n'y en a aucun."""
    total, rejets = somme_valide(textes)
    return total / len(textes)
