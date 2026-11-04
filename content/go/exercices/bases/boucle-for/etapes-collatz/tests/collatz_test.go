package collatz

import "testing"

func TestEtapesDeUnVautZero(t *testing.T) {
	if got := Etapes(1); got != 0 {
		t.Errorf("Etapes(1) = %d, attendu 0", got)
	}
}

func TestEtapesSuitLaSuite(t *testing.T) {
	cas := []struct{ n, attendu int }{{2, 1}, {6, 8}, {7, 16}, {27, 111}}
	for _, c := range cas {
		if got := Etapes(c.n); got != c.attendu {
			t.Errorf("Etapes(%d) = %d, attendu %d", c.n, got, c.attendu)
		}
	}
}

func TestEtapesRefuseLesNombresNonPositifs(t *testing.T) {
	for _, n := range []int{0, -5} {
		if got := Etapes(n); got != -1 {
			t.Errorf("Etapes(%d) = %d, attendu -1", n, got)
		}
	}
}
