// Package service expose les routes HTTP d'un petit service.
package service

import "net/http"

// Routes renvoie le routeur du service.
func Routes() http.Handler {
	return http.NewServeMux()
}
