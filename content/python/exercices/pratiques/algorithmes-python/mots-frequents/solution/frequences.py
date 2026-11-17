"""Les mots les plus fréquents d'un texte."""
from collections import Counter


def plus_frequents(mots, k):
    """Les k mots les plus fréquents, du plus au moins fréquent ; à égalité, dans l'ordre alphabétique."""
    if k <= 0:
        return []
    compte = Counter(mot.lower() for mot in mots)
    return sorted(compte, key=lambda mot: (-compte[mot], mot))[:k]
