"""Appliquer une fonction à des éléments, en parallèle, sans qu'une erreur arrête le lot."""


def traiter(elements, fonction, max_workers=4):
    """Renvoie (resultats, erreurs) : {element: valeur} et {element: exception}."""
    resultats, erreurs = {}, {}
    for element in elements:
        resultats[element] = fonction(element)
    return resultats, erreurs
