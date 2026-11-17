"""Relance les clients dont une facture est en retard."""
import datetime


def aujourd_hui():
    """La date du jour. Les tests la remplacent avec patch."""
    return datetime.date.today()


def relancer(factures, envoyer):
    """Envoie une relance pour chaque facture impayée dont l'échéance est passée.

    envoyer(client, message) peut lever une ConnectionError. Renvoie le nombre de relances envoyées.
    """
    jour = aujourd_hui()
    envoyees = 0
    for facture in factures:
        if facture["payee"] or facture["echeance"] >= jour:
            continue
        message = f"Rappel : facture de {facture['montant']} € échue le {facture['echeance'].isoformat()}"
        try:
            envoyer(facture["client"], message)
        except ConnectionError:
            continue
        envoyees += 1
    return envoyees
