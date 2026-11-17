"""Une playlist qui se comporte comme une séquence de titres."""


class Playlist:
    """Une liste de titres, avec un nom."""

    def __init__(self, nom, titres=None):
        self.nom = nom
        self.titres = list(titres) if titres is not None else []

    def ajouter(self, titre):
        """Ajoute un titre à la fin de la playlist."""
        self.titres.append(titre)

    def __len__(self):
        return len(self.titres)

    def __getitem__(self, indice):
        if isinstance(indice, slice):
            return Playlist(self.nom, self.titres[indice])
        return self.titres[indice]

    def __contains__(self, titre):
        cherche = titre.casefold()
        return any(t.casefold() == cherche for t in self.titres)

    def __iter__(self):
        return iter(self.titres)
