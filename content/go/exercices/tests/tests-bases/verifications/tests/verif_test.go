package verif

import (
	"errors"
	"fmt"
	"testing"
)

// fauxT enregistre ce que les fonctions d'aide lui demandent, sans faire échouer le vrai test.
type fauxT struct {
	testing.TB
	helper  int
	erreurs []string
	fatals  []string
}

func (f *fauxT) Helper() { f.helper++ }

func (f *fauxT) Errorf(format string, args ...any) {
	f.erreurs = append(f.erreurs, fmt.Sprintf(format, args...))
}

func (f *fauxT) Fatalf(format string, args ...any) {
	f.fatals = append(f.fatals, fmt.Sprintf(format, args...))
}

func TestEgalNeSignaleRienQuandLesValeursSontEgales(t *testing.T) {
	f := &fauxT{}
	Egal(f, 4, 4)
	Egal(f, "go", "go")
	if len(f.erreurs) != 0 || len(f.fatals) != 0 {
		t.Errorf("échecs signalés : %v %v, attendu aucun", f.erreurs, f.fatals)
	}
}

func TestEgalSignaleLEcartAvecErrorf(t *testing.T) {
	f := &fauxT{}
	Egal(f, 3, 4)
	if len(f.erreurs) != 1 || f.erreurs[0] != "obtenu 3, attendu 4" {
		t.Errorf("Errorf = %q, attendu [\"obtenu 3, attendu 4\"]", f.erreurs)
	}
	if len(f.fatals) != 0 {
		t.Errorf("Egal a appelé Fatalf : %q ; il doit laisser le test continuer", f.fatals)
	}
}

func TestEgalAvecDesChaines(t *testing.T) {
	f := &fauxT{}
	Egal(f, "java", "go")
	if len(f.erreurs) != 1 || f.erreurs[0] != "obtenu java, attendu go" {
		t.Errorf("Errorf = %q, attendu [\"obtenu java, attendu go\"]", f.erreurs)
	}
}

func TestSansErreurArreteLeTest(t *testing.T) {
	f := &fauxT{}
	SansErreur(f, errors.New("disque plein"))
	if len(f.fatals) != 1 || f.fatals[0] != "erreur inattendue : disque plein" {
		t.Errorf("Fatalf = %q, attendu [\"erreur inattendue : disque plein\"]", f.fatals)
	}
}

func TestSansErreurNeSignaleRienSansErreur(t *testing.T) {
	f := &fauxT{}
	SansErreur(f, nil)
	if len(f.erreurs) != 0 || len(f.fatals) != 0 {
		t.Errorf("échecs signalés : %v %v, attendu aucun", f.erreurs, f.fatals)
	}
}

func TestLesDeuxAppellentHelper(t *testing.T) {
	f := &fauxT{}
	Egal(f, 1, 1)
	SansErreur(f, nil)
	if f.helper < 2 {
		t.Errorf("t.Helper() appelé %d fois, attendu dans chaque fonction", f.helper)
	}
}
