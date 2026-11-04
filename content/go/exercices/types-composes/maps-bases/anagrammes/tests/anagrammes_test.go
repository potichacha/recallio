package anagrammes

import (
	"reflect"
	"testing"
)

func TestGrouperLesAnagrammesDansLOrdreDArrivee(t *testing.T) {
	got := Grouper([]string{"chien", "niche", "rat", "chine", "art", "go"})
	want := [][]string{{"chien", "niche", "chine"}, {"rat", "art"}, {"go"}}
	if !reflect.DeepEqual(got, want) {
		t.Errorf("Grouper = %v, attendu %v", got, want)
	}
}

func TestGrouperIgnoreLaCasse(t *testing.T) {
	got := Grouper([]string{"Niche", "chien"})
	want := [][]string{{"Niche", "chien"}}
	if !reflect.DeepEqual(got, want) {
		t.Errorf("Grouper = %v, attendu %v", got, want)
	}
}

func TestGrouperDistingueLesLettresAccentuees(t *testing.T) {
	got := Grouper([]string{"été", "tee"})
	if len(got) != 2 {
		t.Errorf("Grouper = %v, attendu deux groupes (é n'est pas e)", got)
	}
}
