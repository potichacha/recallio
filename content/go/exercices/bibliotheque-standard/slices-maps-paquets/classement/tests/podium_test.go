package classement

import (
	"maps"
	"slices"
	"testing"
)

func TestPodiumTrieParScoreDecroissant(t *testing.T) {
	got := Podium(map[string]int{"max": 10, "bob": 50, "ada": 30}, 3)
	if want := []string{"bob", "ada", "max"}; !slices.Equal(got, want) {
		t.Errorf("Podium = %v, attendu %v", got, want)
	}
}

func TestPodiumDepartageLesExAequoParNom(t *testing.T) {
	got := Podium(map[string]int{"ada": 30, "bob": 50, "eve": 30, "max": 10, "cleo": 30}, 4)
	if want := []string{"bob", "ada", "cleo", "eve"}; !slices.Equal(got, want) {
		t.Errorf("Podium = %v, attendu %v", got, want)
	}
}

func TestPodiumAvecMoinsDeJoueursQueDePlaces(t *testing.T) {
	got := Podium(map[string]int{"ada": 1, "bob": 2}, 5)
	if want := []string{"bob", "ada"}; !slices.Equal(got, want) {
		t.Errorf("Podium = %v, attendu %v", got, want)
	}
}

func TestPodiumSansPlace(t *testing.T) {
	if got := Podium(map[string]int{"ada": 1}, 0); len(got) != 0 {
		t.Errorf("Podium(n = 0) = %v, attendu vide", got)
	}
}

func TestPodiumNeModifiePasLesScores(t *testing.T) {
	scores := map[string]int{"ada": 1, "bob": 2, "eve": 3}
	avant := maps.Clone(scores)
	Podium(scores, 2)
	if !maps.Equal(scores, avant) {
		t.Errorf("scores modifiés : %v, attendu %v", scores, avant)
	}
}
