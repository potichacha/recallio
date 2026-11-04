package urne

import (
	"sync"
	"testing"
)

func TestVoterCompteLesVoix(t *testing.T) {
	u := NouvelleUrne()
	u.Voter("Ada")
	u.Voter("Ada")
	u.Voter("Grace")
	if u.Voix("Ada") != 2 || u.Voix("Grace") != 1 {
		t.Errorf("Ada = %d, Grace = %d, attendu 2 et 1", u.Voix("Ada"), u.Voix("Grace"))
	}
}

func TestVoixDUnCandidatInconnu(t *testing.T) {
	if n := NouvelleUrne().Voix("Linus"); n != 0 {
		t.Errorf("Voix(inconnu) = %d, attendu 0", n)
	}
}

func TestTotalAdditionneToutesLesVoix(t *testing.T) {
	u := NouvelleUrne()
	for _, c := range []string{"Ada", "Grace", "Ada", "Barbara"} {
		u.Voter(c)
	}
	if n := u.Total(); n != 4 {
		t.Errorf("Total = %d, attendu 4", n)
	}
}

func TestAucuneVoixNeSePerdEnParallele(t *testing.T) {
	u := NouvelleUrne()
	candidats := []string{"Ada", "Grace", "Barbara", "Margaret"}
	var wg sync.WaitGroup
	for i := range 400 {
		wg.Go(func() {
			for range 50 {
				u.Voter(candidats[i%len(candidats)])
				u.Voix(candidats[(i+1)%len(candidats)])
			}
		})
	}
	wg.Wait()
	for _, c := range candidats {
		if n := u.Voix(c); n != 5000 {
			t.Errorf("Voix(%s) = %d, attendu 5000", c, n)
		}
	}
	if n := u.Total(); n != 20000 {
		t.Errorf("Total = %d, attendu 20000", n)
	}
}
