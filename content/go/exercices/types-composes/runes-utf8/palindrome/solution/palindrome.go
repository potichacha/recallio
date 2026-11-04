// Package palindrome reconnaît les phrases qui se lisent dans les deux sens.
package palindrome

import "unicode"

// EstPalindrome dit si s se lit de la même façon dans les deux sens, en ne gardant que les lettres et les chiffres,
// sans tenir compte de la casse.
func EstPalindrome(s string) bool {
	var lettres []rune
	for _, r := range s {
		if unicode.IsLetter(r) || unicode.IsDigit(r) {
			lettres = append(lettres, unicode.ToLower(r))
		}
	}
	for i, j := 0, len(lettres)-1; i < j; i, j = i+1, j-1 {
		if lettres[i] != lettres[j] {
			return false
		}
	}
	return true
}
