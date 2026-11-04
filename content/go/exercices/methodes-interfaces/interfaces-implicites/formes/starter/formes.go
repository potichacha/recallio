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

func (t Triangle) Aire() float64      { return 0 }
func (t Triangle) Perimetre() float64 { return 0 }

// PlusGrande renvoie la forme de plus grande aire, et false si la liste est vide.
func PlusGrande(formes []Forme) (Forme, bool) {
	return nil, false
}
