import statistics

import pytest

from verifications import verifier_moyenne


def moyenne_correcte(notes):
    if not notes:
        raise ValueError("aucune note")
    return sum(notes) / len(notes)


def division_entiere(notes):
    if not notes:
        raise ValueError("aucune note")
    return sum(notes) // len(notes)


def oublie_la_premiere_note(notes):
    if not notes:
        raise ValueError("aucune note")
    return sum(notes[1:]) / len(notes)


def mediane(notes):
    if not notes:
        raise ValueError("aucune note")
    return statistics.median(notes)


def zero_sans_note(notes):
    return sum(notes) / len(notes) if notes else 0


def division_par_zero_sans_note(notes):
    return sum(notes) / len(notes)


def rejette(implementation):
    """Vrai si verifier_moyenne échoue face à cette implémentation (un assert faux, ou une exception)."""
    try:
        verifier_moyenne(implementation)
    except (Exception, pytest.fail.Exception):
        return True
    return False


def test_accepte_une_moyenne_correcte():
    verifier_moyenne(moyenne_correcte)


@pytest.mark.parametrize("fautive", [
    division_entiere,
    oublie_la_premiere_note,
    mediane,
    zero_sans_note,
    division_par_zero_sans_note,
])
def test_rejette_une_moyenne_fautive(fautive):
    assert rejette(fautive), f"verifier_moyenne laisse passer {fautive.__name__}"
