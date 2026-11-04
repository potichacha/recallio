package seq

import (
	"iter"
	"slices"
	"testing"
)

// naturels renvoie 1, 2, 3... sans fin, et compte les éléments produits.
func naturels(produits *int) iter.Seq[int] {
	return func(yield func(int) bool) {
		for i := 1; ; i++ {
			*produits++
			if !yield(i) {
				return
			}
		}
	}
}

func pair(n int) bool { return n%2 == 0 }

func TestFiltrerGardeLesElementsChoisis(t *testing.T) {
	got := slices.Collect(Filtrer(slices.Values([]int{1, 2, 3, 4, 5, 6}), pair))
	if !slices.Equal(got, []int{2, 4, 6}) {
		t.Errorf("Filtrer = %v, attendu [2 4 6]", got)
	}
}

func TestPrendreLimiteLeNombreDElements(t *testing.T) {
	got := slices.Collect(Prendre(slices.Values([]string{"a", "b", "c", "d"}), 2))
	if !slices.Equal(got, []string{"a", "b"}) {
		t.Errorf("Prendre = %v, attendu [a b]", got)
	}
}

func TestPrendrePlusQueDisponible(t *testing.T) {
	got := slices.Collect(Prendre(slices.Values([]int{1, 2}), 5))
	if !slices.Equal(got, []int{1, 2}) {
		t.Errorf("Prendre = %v, attendu [1 2]", got)
	}
}

func TestPrendreZeroNeDemandeRien(t *testing.T) {
	produits := 0
	got := slices.Collect(Prendre(naturels(&produits), 0))
	if len(got) != 0 || produits != 0 {
		t.Errorf("Prendre(0) = %v avec %d éléments produits, attendu aucun", got, produits)
	}
}

func TestLesDeuxSontParesseuxSurUneSequenceInfinie(t *testing.T) {
	produits := 0
	got := slices.Collect(Prendre(Filtrer(naturels(&produits), pair), 3))
	if !slices.Equal(got, []int{2, 4, 6}) {
		t.Errorf("Prendre(Filtrer(naturels, pair), 3) = %v, attendu [2 4 6]", got)
	}
	if produits != 6 {
		t.Errorf("%d éléments produits par la source, attendu 6", produits)
	}
}

func TestLArretDeLaBoucleRemonteJusquALaSource(t *testing.T) {
	produits := 0
	var vus []int
	for v := range Filtrer(naturels(&produits), pair) {
		vus = append(vus, v)
		if v == 4 {
			break
		}
	}
	if !slices.Equal(vus, []int{2, 4}) || produits != 4 {
		t.Errorf("vus = %v avec %d éléments produits, attendu [2 4] et 4", vus, produits)
	}
}
