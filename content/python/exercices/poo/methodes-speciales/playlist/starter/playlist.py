"""Une playlist qui se comporte comme une séquence de titres."""


class Playlist:
    """Une liste de titres, avec un nom."""

    def __init__(self, nom, titres=None):
        self.nom = nom
        self.titres = titres

    def ajouter(self, titre):
        """Ajoute un titre à la fin de la playlist."""
        self.titres.append(titre)
