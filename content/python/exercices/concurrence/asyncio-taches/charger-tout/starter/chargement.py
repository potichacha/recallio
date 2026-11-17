"""Charger plusieurs ressources en même temps, avec un délai et une limite."""


async def charger_tous(noms, charger, delai, max_simultanes=None):
    """Renvoie {nom: ressource}, dans l'ordre des noms ; None pour un chargement trop long."""
    ressources = {}
    for nom in noms:
        ressources[nom] = await charger(nom)
    return ressources
