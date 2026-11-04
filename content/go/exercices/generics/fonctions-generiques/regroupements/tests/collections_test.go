package collections

import (
	"reflect"
	"slices"
	"testing"
)

func TestGrouperParPremiereLettre(t *testing.T) {
	got := Grouper([]string{"ada", "bob", "alan"}, func(s string) byte { return s[0] })
	want := map[byte][]string{'a': {"ada", "alan"}, 'b': {"bob"}}
	if !reflect.DeepEqual(got, want) {
		t.Errorf("Grouper = %v, attendu %v", got, want)
	}
}

type commande struct {
	Client string
	Total  int
}

func TestGrouperDesStructsParChamp(t *testing.T) {
	cmds := []commande{{"ada", 10}, {"bob", 5}, {"ada", 7}}
	got := Grouper(cmds, func(c commande) string { return c.Client })
	if len(got) != 2 || len(got["ada"]) != 2 || got["ada"][1].Total != 7 {
		t.Errorf("Grouper = %v", got)
	}
}

func TestUniqueGardeLaPremiereOccurrenceDansLOrdre(t *testing.T) {
	if got := Unique([]int{3, 1, 3, 2, 1}); !slices.Equal(got, []int{3, 1, 2}) {
		t.Errorf("Unique = %v, attendu [3 1 2]", got)
	}
	if got := Unique([]string{"go", "go"}); !slices.Equal(got, []string{"go"}) {
		t.Errorf("Unique = %v, attendu [go]", got)
	}
}

func TestUniqueDUnSliceVide(t *testing.T) {
	if got := Unique([]int{}); len(got) != 0 {
		t.Errorf("Unique([]) = %v", got)
	}
}
