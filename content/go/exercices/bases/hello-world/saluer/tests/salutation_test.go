package salutation

import "testing"

func TestSaluerAvecUnPrenom(t *testing.T) {
	if got := Saluer("Ada"); got != "Bonjour, Ada !" {
		t.Errorf(`Saluer("Ada") = %q, attendu "Bonjour, Ada !"`, got)
	}
}

func TestSaluerIgnoreLesEspacesAutourDuPrenom(t *testing.T) {
	if got := Saluer("  Ada "); got != "Bonjour, Ada !" {
		t.Errorf(`Saluer("  Ada ") = %q, attendu "Bonjour, Ada !"`, got)
	}
}

func TestSaluerSansPrenom(t *testing.T) {
	for _, prenom := range []string{"", "   "} {
		if got := Saluer(prenom); got != "Bonjour !" {
			t.Errorf("Saluer(%q) = %q, attendu \"Bonjour !\"", prenom, got)
		}
	}
}
