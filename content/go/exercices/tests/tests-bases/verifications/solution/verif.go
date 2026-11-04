// Package verif fournit des fonctions d'aide pour les tests.
package verif

import "testing"

// Egal signale un échec si obtenu et attendu diffèrent, et laisse le test continuer.
func Egal[T comparable](t testing.TB, obtenu, attendu T) {
	t.Helper()
	if obtenu != attendu {
		t.Errorf("obtenu %v, attendu %v", obtenu, attendu)
	}
}

// SansErreur arrête le test si err n'est pas nil.
func SansErreur(t testing.TB, err error) {
	t.Helper()
	if err != nil {
		t.Fatalf("erreur inattendue : %v", err)
	}
}
