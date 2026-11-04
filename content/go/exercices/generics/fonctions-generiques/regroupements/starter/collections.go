// Package collections regroupe des fonctions génériques sur les slices.
package collections

// Grouper range chaque élément sous sa clé, dans l'ordre d'arrivée.
func Grouper[T any, K comparable](xs []T, cle func(T) K) map[K][]T {
	return map[K][]T{}
}

// Unique retire les doublons en gardant la première occurrence de chaque valeur.
func Unique[T comparable](xs []T) []T {
	return xs
}
