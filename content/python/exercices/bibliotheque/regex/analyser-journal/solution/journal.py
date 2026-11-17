import re

LIGNE = re.compile(
    r"(?P<date>\d{4}-\d{2}-\d{2}) "
    r"(?P<heure>\d{2}:\d{2}:\d{2}) "
    r"(?P<niveau>DEBUG|INFO|WARNING|ERROR) "
    r"\[(?P<module>[a-z0-9_]+)\] "
    r"(?P<message>.+)"
)
EMAIL = re.compile(r"[\w.+-]+@[\w-]+(?:\.[\w-]+)+")


def analyser(ligne: str) -> dict[str, str] | None:
    """Découpe une ligne du journal en ses champs, ou renvoie None si elle n'a pas la bonne forme."""
    trouve = LIGNE.fullmatch(ligne)
    return trouve.groupdict() if trouve else None


def masquer_emails(texte: str) -> str:
    """Remplace chaque adresse e-mail du texte par <email>."""
    return EMAIL.sub("<email>", texte)
