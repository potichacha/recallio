"""Regroupe des données dans des dictionnaires."""


def par_initiale(mots):
    """Associe à chaque initiale, en minuscule, la liste de ses mots, dans leur ordre d'arrivée.

    Les mots vides sont ignorés.
    """
    groupes = {}
    for mot in mots:
        if mot:
            groupes.setdefault(mot[0].lower(), []).append(mot)
    return groupes


def inverser(notes):
    """Inverse {nom: note} en {note: [noms]}, les noms dans l'ordre du dictionnaire reçu."""
    inverse = {}
    for nom, note in notes.items():
        inverse.setdefault(note, []).append(nom)
    return inverse
