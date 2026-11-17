import json
from datetime import date


def serialiser(commande: dict) -> str:
    """Renvoie la commande en JSON : date ISO, tags en liste triée, clés triées, accents conservés."""
    donnees = dict(commande)
    donnees["date"] = commande["date"].isoformat()
    donnees["tags"] = sorted(commande["tags"])
    return json.dumps(donnees, sort_keys=True, ensure_ascii=False)


def deserialiser(texte: str) -> dict:
    """Relit le JSON produit par serialiser ; lève ValueError si le texte est invalide ou sans "id"."""
    donnees = json.loads(texte)
    if not isinstance(donnees, dict) or "id" not in donnees:
        raise ValueError("commande sans identifiant")
    donnees["date"] = date.fromisoformat(donnees["date"])
    donnees["tags"] = set(donnees["tags"])
    return donnees
