"""Une playlist : une liste de titres, que ces fonctions modifient sur place."""


def ajouter(playlist, titres):
    """Ajoute à la fin de la playlist chacun des titres de la liste titres."""
    playlist.extend(titres)


def deplacer(playlist, titre, position):
    """Retire le titre et le réinsère à l'indice position ; ValueError s'il est absent."""
    playlist.remove(titre)
    playlist.insert(position, titre)


def retirer_tout(playlist, titre):
    """Retire toutes les occurrences du titre, et renvoie leur nombre."""
    nombre = playlist.count(titre)
    for _ in range(nombre):
        playlist.remove(titre)
    return nombre
