"""Deux gestionnaires de contexte : une transaction sur un dictionnaire, et ignorer des exceptions."""

from contextlib import contextmanager


class Transaction:
    """Applique les changements d'un bloc with à un dictionnaire, seulement si le bloc réussit."""

    def __init__(self, donnees):
        self.donnees = donnees
        self._brouillon = None

    def __enter__(self):
        self._brouillon = dict(self.donnees)
        return self._brouillon

    def __exit__(self, exc_type, exc, tb):
        if exc_type is None:
            self.donnees.clear()
            self.donnees.update(self._brouillon)
        return False


@contextmanager
def ignorer(*types):
    """Ignore les exceptions des types donnés, levées dans le bloc with."""
    try:
        yield
    except types:
        pass
