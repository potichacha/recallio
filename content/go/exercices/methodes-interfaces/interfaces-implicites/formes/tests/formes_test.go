package formes

import (
	"math"
	"testing"
)

func procheDe(a, b float64) bool { return math.Abs(a-b) < 1e-9 }

func TestUnTriangleEstUneForme(t *testing.T) {
	var f Forme = Triangle{3, 4, 5}
	if !procheDe(f.Perimetre(), 12) {
		t.Errorf("Perimetre = %v, attendu 12", f.Perimetre())
	}
	if !procheDe(f.Aire(), 6) {
		t.Errorf("Aire = %v, attendu 6", f.Aire())
	}
}

func TestPlusGrandeRenvoieLaFormeDePlusGrandeAire(t *testing.T) {
	formes := []Forme{Triangle{3, 4, 5}, Rectangle{2, 4}, Cercle{1}}
	f, ok := PlusGrande(formes)
	if !ok {
		t.Fatal("PlusGrande a renvoyé false sur une liste non vide")
	}
	if _, estRectangle := f.(Rectangle); !estRectangle {
		t.Errorf("PlusGrande = %#v, attendu le rectangle d'aire 8", f)
	}
}

func TestPlusGrandeDUneListeVide(t *testing.T) {
	if f, ok := PlusGrande(nil); ok || f != nil {
		t.Errorf("PlusGrande(nil) = %v, %v, attendu nil, false", f, ok)
	}
}
