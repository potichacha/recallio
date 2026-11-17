"""Des comptages de mots et de lettres."""

from collections import Counter


def mots_frequents(texte, n):
    """Renvoie les n mots les plus fréquents, en minuscules ; à égalité, le premier apparu passe devant."""
    compte = Counter(texte.lower().split())
    return [mot for mot, _ in compte.most_common(n)]


def sont_anagrammes(a, b):
    """Dit si a et b ont les mêmes lettres, autant de fois chacune, sans compter espaces ni majuscules."""
    return Counter(a.lower().replace(" ", "")) == Counter(b.lower().replace(" ", ""))
