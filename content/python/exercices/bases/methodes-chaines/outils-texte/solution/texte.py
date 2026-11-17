def en_slug(titre):
    """Renvoie le titre en minuscules, les mots séparés par un tiret."""
    return "-".join(titre.lower().split())


def extension(nom_fichier):
    """Renvoie l'extension, en minuscules et sans le point ; la chaîne vide s'il n'y en a pas."""
    position = nom_fichier.rfind(".")
    if position == -1:
        return ""
    return nom_fichier[position + 1:].lower()


def nettoyer_telephone(numero):
    """Renvoie le numéro sans espaces, tirets ni points."""
    return numero.replace(" ", "").replace("-", "").replace(".", "")
