"""Découpe un itérable en paquets."""


def par_paquets(elements, taille):
    """Produit les éléments par listes de `taille` éléments ; la dernière peut être plus courte."""
    if taille < 1:
        raise ValueError("la taille d'un paquet doit valoir au moins 1")
    paquet = []
    for element in elements:
        paquet.append(element)
        if len(paquet) == taille:
            yield paquet
            paquet = []
    if paquet:
        yield paquet
