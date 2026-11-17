def moyenne(notes):
    """Renvoie la moyenne des notes, ou None s'il n'y en a aucune."""
    if not notes:
        return None
    return sum(notes) / len(notes)


def mention(valeur):
    """Renvoie la mention d'une moyenne sur 20, ou None s'il n'y a pas de moyenne."""
    if valeur is None:
        return None
    if valeur >= 16:
        return "très bien"
    if valeur >= 14:
        return "bien"
    if valeur >= 12:
        return "assez bien"
    if valeur >= 10:
        return "passable"
    return "insuffisant"
