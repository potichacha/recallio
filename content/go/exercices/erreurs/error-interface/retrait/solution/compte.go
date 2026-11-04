// Package banque gère des comptes en centimes.
package banque

import (
	"errors"
	"fmt"
)

// ErrSoldeInsuffisant signale un retrait supérieur au solde.
var ErrSoldeInsuffisant = errors.New("solde insuffisant")

// ErrMontant signale un montant de retrait nul ou négatif.
type ErrMontant struct {
	Montant int
}

func (e *ErrMontant) Error() string { return fmt.Sprintf("montant invalide : %d", e.Montant) }

// Compte est un compte bancaire dont le solde est en centimes.
type Compte struct {
	Solde int
}

// Retirer débite le compte, ou renvoie une erreur sans toucher au solde.
func (c *Compte) Retirer(montant int) error {
	if montant <= 0 {
		return &ErrMontant{Montant: montant}
	}
	if montant > c.Solde {
		return fmt.Errorf("retrait de %d (solde %d) : %w", montant, c.Solde, ErrSoldeInsuffisant)
	}
	c.Solde -= montant
	return nil
}
