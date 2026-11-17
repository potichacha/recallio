"""Des vérifications, écrites avec assert, d'une fonction moyenne(notes)."""
import pytest


def verifier_moyenne(moyenne):
    """Vérifie que moyenne(notes) respecte sa spécification ; échoue sinon."""
    assert moyenne([10, 20]) == 15
    assert moyenne([1, 2]) == 1.5
    assert moyenne([1, 2, 6]) == 3
    assert moyenne([7]) == 7
    with pytest.raises(ValueError):
        moyenne([])
