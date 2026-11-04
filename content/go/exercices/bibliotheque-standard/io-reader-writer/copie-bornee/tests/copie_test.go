package copie

import (
	"bytes"
	"errors"
	"io"
	"strings"
	"testing"
)

func TestCopierUneSourceSousLaLimite(t *testing.T) {
	var dst bytes.Buffer
	n, err := Copier(&dst, strings.NewReader("bonjour"), 10)
	if n != 7 || err != nil || dst.String() != "bonjour" {
		t.Errorf("Copier = %d, %v, %q ; attendu 7, nil, \"bonjour\"", n, err, dst.String())
	}
}

func TestCopierUneSourceExactementALaLimite(t *testing.T) {
	var dst bytes.Buffer
	n, err := Copier(&dst, strings.NewReader("bonjour"), 7)
	if n != 7 || err != nil {
		t.Errorf("Copier = %d, %v ; attendu 7, nil", n, err)
	}
}

func TestCopierRefuseUneSourceTropGrosse(t *testing.T) {
	var dst bytes.Buffer
	n, err := Copier(&dst, strings.NewReader("bonjour"), 3)
	if !errors.Is(err, ErrTropGros) {
		t.Errorf("err = %v, attendu ErrTropGros", err)
	}
	if n != 3 || dst.String() != "bon" {
		t.Errorf("Copier a écrit %d octets (%q), attendu 3 (\"bon\")", n, dst.String())
	}
}

func TestCopierLitUneSourceOctetParOctet(t *testing.T) {
	var dst bytes.Buffer
	n, err := Copier(&dst, &octetParOctet{r: strings.NewReader("gopher")}, 6)
	if n != 6 || err != nil || dst.String() != "gopher" {
		t.Errorf("Copier = %d, %v, %q ; attendu 6, nil, \"gopher\"", n, err, dst.String())
	}
}

func TestCopierRenvoieLErreurDeLecture(t *testing.T) {
	panne := errors.New("disque illisible")
	var dst bytes.Buffer
	_, err := Copier(&dst, lecteurEnPanne{panne}, 100)
	if !errors.Is(err, panne) {
		t.Errorf("err = %v, attendu %v", err, panne)
	}
}

func TestCopierUneGrosseSourceEnFlux(t *testing.T) {
	const taille = 50 << 20 // 50 Mo
	var compte compteur
	n, err := Copier(&compte, &octetParOctet{r: &zeros{reste: taille}, max: 1000}, taille)
	if n != taille || err != nil || compte.n != taille {
		t.Errorf("Copier = %d, %v (%d écrits) ; attendu %d, nil", n, err, compte.n, taille)
	}
}

// zeros produit reste octets nuls sans jamais les stocker.
type zeros struct{ reste int64 }

func (z *zeros) Read(p []byte) (int, error) {
	if z.reste == 0 {
		return 0, io.EOF
	}
	n := min(int64(len(p)), z.reste)
	clear(p[:n])
	z.reste -= n
	return int(n), nil
}

// compteur compte les octets écrits sans les garder.
type compteur struct{ n int64 }

func (c *compteur) Write(p []byte) (int, error) {
	c.n += int64(len(p))
	return len(p), nil
}

// octetParOctet ne rend que quelques octets à chaque lecture (un seul par défaut), comme un réseau lent.
type octetParOctet struct {
	r   io.Reader
	max int
}

func (o *octetParOctet) Read(p []byte) (int, error) {
	if len(p) == 0 {
		return 0, nil
	}
	return o.r.Read(p[:min(len(p), max(o.max, 1))])
}

// lecteurEnPanne échoue à chaque lecture.
type lecteurEnPanne struct{ err error }

func (l lecteurEnPanne) Read([]byte) (int, error) { return 0, l.err }
