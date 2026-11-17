"""Des transformations de listes, chacune en une compréhension."""


def carres_des_pairs(nombres):
    """Renvoie les carrés des nombres pairs, dans l'ordre."""
    return [n * n for n in nombres if n % 2 == 0]


def initiales(noms):
    """Renvoie l'initiale en majuscule de chaque nom ; les noms vides ou faits d'espaces sont ignorés."""
    return [nom.strip()[0].upper() for nom in noms if nom.strip()]


def bornees(notes):
    """Renvoie une nouvelle liste où chaque note est ramenée entre 0 et 20."""
    return [min(max(note, 0), 20) for note in notes]
