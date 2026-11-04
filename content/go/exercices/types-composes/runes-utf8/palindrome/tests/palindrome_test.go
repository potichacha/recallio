package palindrome

import "testing"

func TestDesPalindromes(t *testing.T) {
	for _, s := range []string{"", "kayak", "Engage le jeu que je le gagne", "Été", "1221"} {
		if !EstPalindrome(s) {
			t.Errorf("EstPalindrome(%q) = false, attendu true", s)
		}
	}
}

func TestDesPhrasesQuiNeSontPasDesPalindromes(t *testing.T) {
	for _, s := range []string{"bonjour", "Ésope reste ici et se repose", "123"} {
		if EstPalindrome(s) {
			t.Errorf("EstPalindrome(%q) = true, attendu false", s)
		}
	}
}
