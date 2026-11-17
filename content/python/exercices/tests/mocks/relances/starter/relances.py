"""Relance les clients dont une facture est en retard."""
import datetime


def aujourd_hui():
    """La date du jour. Les tests la remplacent avec patch."""
    return datetime.date.today()


def relancer(factures, envoyer):
    """Envoie une relance pour chaque facture impayée dont l'échéance est passée.

    envoyer(client, message) peut lever une ConnectionError. Renvoie le nombre de relances envoyées.
    """
    raise NotImplementedError
