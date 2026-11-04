// Package collections regroupe des fonctions génériques sur les slices.
package collections

// Grouper range chaque élément sous sa clé, dans l'ordre d'arrivée.
func Grouper[T any, K comparable](xs []T, cle func(T) K) map[K][]T {
	groupes := make(map[K][]T)
	for _, x := range xs {
		k := cle(x)
		groupes[k] = append(groupes[k], x)
	}
	return groupes
}

// Unique retire les doublons en gardant la première occurrence de chaque valeur.
func Unique[T comparable](xs []T) []T {
	vus := make(map[T]struct{}, len(xs))
	res := make([]T, 0, len(xs))
	for _, x := range xs {
		if _, ok := vus[x]; ok {
			continue
		}
		vus[x] = struct{}{}
		res = append(res, x)
	}
	return res
}
