package banque

import (
	"errors"
	"testing"
)

func TestRetirerDebiteLeCompte(t *testing.T) {
	c := &Compte{Solde: 1000}
	if err := c.Retirer(300); err != nil {
		t.Fatalf("erreur inattendue : %v", err)
	}
	if c.Solde != 700 {
		t.Errorf("Solde = %d, attendu 700", c.Solde)
	}
}

func TestRetirerPlusQueLeSoldeEnveloppeErrSoldeInsuffisant(t *testing.T) {
	c := &Compte{Solde: 300}
	err := c.Retirer(500)
	if !errors.Is(err, ErrSoldeInsuffisant) {
		t.Fatalf("err = %v, attendu une erreur qui enveloppe ErrSoldeInsuffisant", err)
	}
	if want := "retrait de 500 (solde 300) : solde insuffisant"; err.Error() != want {
		t.Errorf("message = %q, attendu %q", err.Error(), want)
	}
	if c.Solde != 300 {
		t.Errorf("Solde = %d après un retrait refusé, attendu 300", c.Solde)
	}
}

func TestRetirerUnMontantInvalideRenvoieUneErrMontant(t *testing.T) {
	c := &Compte{Solde: 300}
	for _, montant := range []int{0, -50} {
		err := c.Retirer(montant)
		var em *ErrMontant
		if !errors.As(err, &em) || em.Montant != montant {
			t.Errorf("Retirer(%d) = %v, attendu une *ErrMontant de montant %d", montant, err, montant)
		}
	}
	if c.Solde != 300 {
		t.Errorf("Solde = %d, attendu 300", c.Solde)
	}
}
