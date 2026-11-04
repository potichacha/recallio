// Package service expose les routes HTTP d'un petit service.
package service

import (
	"encoding/json"
	"fmt"
	"net/http"
	"unicode/utf8"
)

// Routes renvoie le routeur du service.
func Routes() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /bonjour", bonjour)
	mux.HandleFunc("GET /mots/{mot}", mot)
	mux.HandleFunc("GET /sante", sante)
	return mux
}

func bonjour(w http.ResponseWriter, r *http.Request) {
	nom := r.URL.Query().Get("nom")
	if nom == "" {
		nom = "inconnu"
	}
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	fmt.Fprintf(w, "Bonjour, %s !", nom)
}

type reponseMot struct {
	Mot      string `json:"mot"`
	Longueur int    `json:"longueur"`
}

func mot(w http.ResponseWriter, r *http.Request) {
	m := r.PathValue("mot")
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(reponseMot{Mot: m, Longueur: utf8.RuneCountInString(m)})
}

func sante(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNoContent)
}
