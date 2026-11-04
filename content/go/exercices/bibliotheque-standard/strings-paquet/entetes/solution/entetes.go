// Package entetes lit des en-têtes « Nom: valeur ».
package entetes

import (
	"fmt"
	"strings"
)

// Parser lit une ligne « Nom: valeur » par en-tête et renvoie les valeurs par nom en minuscules.
func Parser(texte string) (map[string]string, error) {
	entetes := map[string]string{}
	numero := 0
	for ligne := range strings.Lines(texte) {
		numero++
		ligne = strings.TrimSpace(ligne)
		if ligne == "" {
			continue
		}
		nom, valeur, ok := strings.Cut(ligne, ":")
		nom = strings.TrimSpace(nom)
		if !ok || nom == "" {
			return nil, fmt.Errorf("ligne %d : en-tête invalide %q", numero, ligne)
		}
		entetes[strings.ToLower(nom)] = strings.TrimSpace(valeur)
	}
	return entetes, nil
}
