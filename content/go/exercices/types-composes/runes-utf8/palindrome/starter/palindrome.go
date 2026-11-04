// Package palindrome reconnaît les phrases qui se lisent dans les deux sens.
package palindrome

// EstPalindrome dit si s se lit de la même façon dans les deux sens, en ne gardant que les lettres et les chiffres,
// sans tenir compte de la casse.
func EstPalindrome(s string) bool {
	for i := 0; i < len(s)/2; i++ {
		if s[i] != s[len(s)-1-i] {
			return false
		}
	}
	return true
}
