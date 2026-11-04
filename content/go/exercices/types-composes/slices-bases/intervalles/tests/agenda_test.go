package agenda

import (
	"slices"
	"testing"
)

func TestFusionnerLesIntervallesQuiSeChevauchent(t *testing.T) {
	got := Fusionner([]Intervalle{{1, 3}, {2, 6}, {8, 10}})
	if want := []Intervalle{{1, 6}, {8, 10}}; !slices.Equal(got, want) {
		t.Errorf("Fusionner = %v, attendu %v", got, want)
	}
}

func TestFusionnerLesIntervallesQuiSeTouchent(t *testing.T) {
	got := Fusionner([]Intervalle{{1, 4}, {4, 5}})
	if want := []Intervalle{{1, 5}}; !slices.Equal(got, want) {
		t.Errorf("Fusionner = %v, attendu %v", got, want)
	}
}

func TestFusionnerUneEntreeDansLeDesordreSansLaModifier(t *testing.T) {
	entree := []Intervalle{{8, 10}, {1, 3}, {2, 6}}
	avant := slices.Clone(entree)
	got := Fusionner(entree)
	if want := []Intervalle{{1, 6}, {8, 10}}; !slices.Equal(got, want) {
		t.Errorf("Fusionner = %v, attendu %v", got, want)
	}
	if !slices.Equal(entree, avant) {
		t.Errorf("l'entrée a été modifiée : %v", entree)
	}
}

func TestFusionnerUneEntreeVide(t *testing.T) {
	if got := Fusionner(nil); len(got) != 0 {
		t.Errorf("Fusionner(nil) = %v, attendu un résultat vide", got)
	}
}
