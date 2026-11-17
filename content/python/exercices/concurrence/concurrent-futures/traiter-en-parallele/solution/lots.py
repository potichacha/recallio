"""Appliquer une fonction à des éléments, en parallèle, sans qu'une erreur arrête le lot."""

from concurrent.futures import ThreadPoolExecutor, as_completed


def traiter(elements, fonction, max_workers=4):
    """Renvoie (resultats, erreurs) : {element: valeur} et {element: exception}."""
    resultats, erreurs = {}, {}
    with ThreadPoolExecutor(max_workers=max_workers) as ex:
        futurs = {ex.submit(fonction, element): element for element in elements}
        for futur in as_completed(futurs):
            element = futurs[futur]
            try:
                resultats[element] = futur.result()
            except Exception as erreur:
                erreurs[element] = erreur
    return resultats, erreurs
