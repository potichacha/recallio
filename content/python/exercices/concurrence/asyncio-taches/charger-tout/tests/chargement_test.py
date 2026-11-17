import asyncio

from chargement import charger_tous

# Délai de garde : un code qui attend pour toujours échoue vite, au lieu de bloquer les tests.
GARDE = 2


def executer(coroutine):
    async def avec_garde():
        return await asyncio.wait_for(coroutine, GARDE)

    return asyncio.run(avec_garde())


def test_renvoie_les_ressources_dans_l_ordre_des_noms():
    async def charger(nom):
        # « c » finit la première, « a » la dernière.
        for _ in range({"a": 3, "b": 2, "c": 1}[nom]):
            await asyncio.sleep(0)
        return nom.upper()

    ressources = executer(charger_tous(["a", "b", "c"], charger, delai=1))
    assert list(ressources.items()) == [("a", "A"), ("b", "B"), ("c", "C")]


def test_lance_les_chargements_en_meme_temps():
    async def scenario():
        # Les trois chargements doivent se retrouver au point de rendez-vous ;
        # l'un après l'autre, le premier attendrait les deux autres pour toujours.
        rendez_vous = asyncio.Barrier(3)

        async def charger(nom):
            await rendez_vous.wait()
            return nom

        return await charger_tous(["a", "b", "c"], charger, delai=1)

    assert executer(scenario()) == {"a": "a", "b": "b", "c": "c"}


def test_un_chargement_trop_long_vaut_none_sans_bloquer_les_autres():
    async def scenario():
        jamais = asyncio.Event()

        async def charger(nom):
            if nom == "lent":
                await jamais.wait()
            return nom

        return await charger_tous(["rapide", "lent", "autre"], charger, delai=0.05)

    assert executer(scenario()) == {"rapide": "rapide", "lent": None, "autre": "autre"}


def test_le_chargement_trop_long_est_annule():
    annules = []

    async def scenario():
        jamais = asyncio.Event()

        async def charger(nom):
            try:
                await jamais.wait()
            except asyncio.CancelledError:
                annules.append(nom)
                raise

        return await charger_tous(["lent"], charger, delai=0.05)

    assert executer(scenario()) == {"lent": None}
    assert annules == ["lent"]


def test_ne_lance_pas_plus_de_max_simultanes_chargements():
    en_cours, maximum = 0, 0

    async def charger(nom):
        nonlocal en_cours, maximum
        en_cours += 1
        maximum = max(maximum, en_cours)
        await asyncio.sleep(0.01)
        en_cours -= 1
        return nom

    ressources = executer(charger_tous(list("abcde"), charger, delai=1, max_simultanes=2))
    assert ressources == {nom: nom for nom in "abcde"}
    assert maximum == 2


def test_sans_noms_renvoie_un_dictionnaire_vide():
    async def charger(nom):
        return nom

    assert executer(charger_tous([], charger, delai=1)) == {}
