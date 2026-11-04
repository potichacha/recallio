package statistiques

import (
	"errors"
	"testing"
)

func TestStatistiquesDUneSerie(t *testing.T) {
	mini, maxi, moyenne, err := Statistiques([]float64{12, 8, 16})
	if err != nil {
		t.Fatalf("erreur inattendue : %v", err)
	}
	if mini != 8 || maxi != 16 || moyenne != 12 {
		t.Errorf("Statistiques = %v, %v, %v, attendu 8, 16, 12", mini, maxi, moyenne)
	}
}

func TestStatistiquesDUneSeuleNote(t *testing.T) {
	mini, maxi, moyenne, err := Statistiques([]float64{14.5})
	if err != nil || mini != 14.5 || maxi != 14.5 || moyenne != 14.5 {
		t.Errorf("Statistiques([14.5]) = %v, %v, %v, %v", mini, maxi, moyenne, err)
	}
}

func TestStatistiquesSansNoteRenvoieErrAucuneNote(t *testing.T) {
	_, _, _, err := Statistiques(nil)
	if !errors.Is(err, ErrAucuneNote) {
		t.Errorf("err = %v, attendu ErrAucuneNote", err)
	}
}
