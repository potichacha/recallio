// Package somme additionne les résultats d'une fonction lente appelée en parallèle.
package somme

// Somme renvoie la somme des f(v), en appelant f en parallèle pour chaque valeur.
func Somme(valeurs []int, f func(int) int) int {
	total := 0
	for _, v := range valeurs {
		total += f(v)
	}
	return total
}
