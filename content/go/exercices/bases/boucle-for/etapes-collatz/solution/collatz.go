// Package collatz calcule la longueur des suites de Collatz.
package collatz

// Etapes renvoie le nombre d'étapes pour que la suite de Collatz partant de n atteigne 1, ou -1 si n <= 0.
func Etapes(n int) int {
	if n <= 0 {
		return -1
	}
	etapes := 0
	for n != 1 {
		if n%2 == 0 {
			n /= 2
		} else {
			n = 3*n + 1
		}
		etapes++
	}
	return etapes
}
