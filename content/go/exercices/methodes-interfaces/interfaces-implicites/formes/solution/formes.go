// Package formes calcule des aires et des périmètres.
package formes

import "math"

// Forme est une figure plane.
type Forme interface {
	Aire() float64
	Perimetre() float64
}

// Rectangle de largeur L et de hauteur H.
type Rectangle struct{ L, H float64 }

func (r Rectangle) Aire() float64      { return r.L * r.H }
func (r Rectangle) Perimetre() float64 { return 2 * (r.L + r.H) }

// Cercle de rayon R.
type Cercle struct{ R float64 }

func (c Cercle) Aire() float64      { return math.Pi * c.R * c.R }
func (c Cercle) Perimetre() float64 { return 2 * math.Pi * c.R }

// Triangle de côtés A, B et C.
type Triangle struct{ A, B, C float64 }

var _ Forme = Triangle{}

// Aire suit la formule de Héron.
func (t Triangle) Aire() float64 {
	p := t.Perimetre() / 2
	return math.Sqrt(p * (p - t.A) * (p - t.B) * (p - t.C))
}

func (t Triangle) Perimetre() float64 { return t.A + t.B + t.C }

// PlusGrande renvoie la forme de plus grande aire, et false si la liste est vide.
func PlusGrande(formes []Forme) (Forme, bool) {
	if len(formes) == 0 {
		return nil, false
	}
	plusGrande := formes[0]
	for _, f := range formes[1:] {
		if f.Aire() > plusGrande.Aire() {
			plusGrande = f
		}
	}
	return plusGrande, true
}
