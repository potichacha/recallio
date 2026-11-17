import json
from datetime import date


def serialiser(commande: dict) -> str:
    """Renvoie la commande en JSON : date ISO, tags en liste triée, clés triées, accents conservés."""
    return json.dumps(commande)


def deserialiser(texte: str) -> dict:
    """Relit le JSON produit par serialiser ; lève ValueError si le texte est invalide ou sans "id"."""
    return json.loads(texte)
