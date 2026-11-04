package entetes

import (
	"maps"
	"strings"
	"testing"
)

func TestParserLitLesEntetes(t *testing.T) {
	got, err := Parser("Host: localhost:8080\n  Content-Type :  text/html \nAccept: */*")
	want := map[string]string{"host": "localhost:8080", "content-type": "text/html", "accept": "*/*"}
	if err != nil || !maps.Equal(got, want) {
		t.Errorf("Parser = %v, %v ; attendu %v", got, err, want)
	}
}

func TestParserIgnoreLesLignesVidesEtLesRetoursWindows(t *testing.T) {
	got, err := Parser("A: 1\r\n\r\n   \nB: 2\r\n")
	want := map[string]string{"a": "1", "b": "2"}
	if err != nil || !maps.Equal(got, want) {
		t.Errorf("Parser = %v, %v ; attendu %v", got, err, want)
	}
}

func TestParserGardeLaDerniereValeur(t *testing.T) {
	got, _ := Parser("X-Essai: 1\nx-essai: 2")
	if got["x-essai"] != "2" || len(got) != 1 {
		t.Errorf("Parser = %v, attendu map[x-essai:2]", got)
	}
}

func TestParserAccepteUneValeurVide(t *testing.T) {
	got, err := Parser("X-Vide:")
	if err != nil || got["x-vide"] != "" || len(got) != 1 {
		t.Errorf("Parser = %v, %v ; attendu map[x-vide:], nil", got, err)
	}
}

func TestParserSignaleLaLigneSansSeparateur(t *testing.T) {
	_, err := Parser("A: 1\n\nsans separateur\nB: 2")
	if err == nil || !strings.Contains(err.Error(), "ligne 3") || !strings.Contains(err.Error(), "sans separateur") {
		t.Errorf("err = %v, attendu une erreur qui cite la ligne 3 et son contenu", err)
	}
}

func TestParserRefuseUnNomVide(t *testing.T) {
	_, err := Parser(": valeur")
	if err == nil || !strings.Contains(err.Error(), "ligne 1") {
		t.Errorf("err = %v, attendu une erreur pour la ligne 1", err)
	}
}
