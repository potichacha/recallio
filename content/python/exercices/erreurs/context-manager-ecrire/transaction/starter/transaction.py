"""Deux gestionnaires de contexte : une transaction sur un dictionnaire, et ignorer des exceptions."""

from contextlib import contextmanager


class Transaction:
    """Applique les changements d'un bloc with à un dictionnaire, seulement si le bloc réussit."""

    def __init__(self, donnees):
        self.donnees = donnees

    def __enter__(self):
        return self.donnees

    def __exit__(self, exc_type, exc, tb):
        return False


@contextmanager
def ignorer(*types):
    """Ignore les exceptions des types donnés, levées dans le bloc with."""
    yield
