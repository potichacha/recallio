"""Convertir les saisies d'un formulaire, sans s'arrêter sur une saisie invalide."""


def lire_entier(texte, defaut=None):
    """L'entier que représente texte, ou defaut si int le refuse."""
    try:
        return int(texte)
    except (ValueError, TypeError):
        return defaut


def somme_valide(textes):
    """Le couple (total des entiers valides, liste des textes refusés)."""
    total = 0
    rejets = []
    for texte in textes:
        try:
            nombre = int(texte)
        except ValueError:
            rejets.append(texte)
        else:
            total += nombre
    return total, rejets


def moyenne(textes):
    """La moyenne des entiers valides, ou None s'il n'y en a aucun."""
    total, rejets = somme_valide(textes)
    try:
        return total / (len(textes) - len(rejets))
    except ZeroDivisionError:
        return None
