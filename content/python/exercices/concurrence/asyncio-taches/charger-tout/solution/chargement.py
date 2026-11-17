"""Charger plusieurs ressources en même temps, avec un délai et une limite."""

import asyncio
import contextlib


async def charger_tous(noms, charger, delai, max_simultanes=None):
    """Renvoie {nom: ressource}, dans l'ordre des noms ; None pour un chargement trop long."""
    limite = asyncio.Semaphore(max_simultanes) if max_simultanes else contextlib.nullcontext()

    async def charger_un(nom):
        async with limite:
            try:
                async with asyncio.timeout(delai):
                    return await charger(nom)
            except TimeoutError:
                return None

    ressources = await asyncio.gather(*(charger_un(nom) for nom in noms))
    return dict(zip(noms, ressources))
