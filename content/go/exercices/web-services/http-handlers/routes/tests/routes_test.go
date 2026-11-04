package service

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func appeler(methode, cible string) *httptest.ResponseRecorder {
	rec := httptest.NewRecorder()
	Routes().ServeHTTP(rec, httptest.NewRequest(methode, cible, nil))
	return rec
}

func TestBonjourSalueParLeNom(t *testing.T) {
	rec := appeler("GET", "/bonjour?nom=Ada")
	if rec.Code != http.StatusOK || rec.Body.String() != "Bonjour, Ada !" {
		t.Errorf("GET /bonjour?nom=Ada = %d %q, attendu 200 \"Bonjour, Ada !\"", rec.Code, rec.Body.String())
	}
}

func TestBonjourSansNom(t *testing.T) {
	rec := appeler("GET", "/bonjour")
	if rec.Body.String() != "Bonjour, inconnu !" {
		t.Errorf("GET /bonjour = %q, attendu \"Bonjour, inconnu !\"", rec.Body.String())
	}
}

func TestMotRenvoieDuJSON(t *testing.T) {
	rec := appeler("GET", "/mots/%C3%A9t%C3%A9") // « été »
	if rec.Code != http.StatusOK {
		t.Fatalf("statut = %d, attendu 200", rec.Code)
	}
	if ct := rec.Header().Get("Content-Type"); !strings.HasPrefix(ct, "application/json") {
		t.Errorf("Content-Type = %q, attendu application/json", ct)
	}
	var corps struct {
		Mot      string `json:"mot"`
		Longueur int    `json:"longueur"`
	}
	if err := json.Unmarshal(rec.Body.Bytes(), &corps); err != nil {
		t.Fatalf("corps %q : %v", rec.Body.String(), err)
	}
	if corps.Mot != "été" || corps.Longueur != 3 {
		t.Errorf("corps = %+v, attendu {Mot:été Longueur:3}", corps)
	}
}

func TestSanteRepondSansCorps(t *testing.T) {
	rec := appeler("GET", "/sante")
	if rec.Code != http.StatusNoContent || rec.Body.Len() != 0 {
		t.Errorf("GET /sante = %d avec %d octets, attendu 204 sans corps", rec.Code, rec.Body.Len())
	}
}

func TestMauvaiseMethode(t *testing.T) {
	if rec := appeler("POST", "/bonjour"); rec.Code != http.StatusMethodNotAllowed {
		t.Errorf("POST /bonjour = %d, attendu 405", rec.Code)
	}
}

func TestCheminInconnu(t *testing.T) {
	if rec := appeler("GET", "/inconnu"); rec.Code != http.StatusNotFound {
		t.Errorf("GET /inconnu = %d, attendu 404", rec.Code)
	}
}
