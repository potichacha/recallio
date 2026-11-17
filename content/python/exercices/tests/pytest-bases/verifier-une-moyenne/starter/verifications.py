"""Des vérifications, écrites avec assert, d'une fonction moyenne(notes)."""
import pytest


def verifier_moyenne(moyenne):
    """Vérifie que moyenne(notes) respecte sa spécification ; échoue sinon."""
    assert moyenne([10, 10]) == 10
