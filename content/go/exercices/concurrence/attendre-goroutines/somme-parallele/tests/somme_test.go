package somme

import (
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

const patience = 500 * time.Millisecond

// simultanes renvoie une fonction qui attend que n appels soient en cours en même temps ;
// le booléen passe à vrai si un appel a dû renoncer à attendre les autres.
func simultanes(n int, f func(int) int) (func(int) int, *atomic.Bool) {
	var demarres sync.WaitGroup
	demarres.Add(n)
	tous := make(chan struct{})
	go func() {
		demarres.Wait()
		close(tous)
	}()
	var renonce atomic.Bool
	return func(v int) int {
		demarres.Done()
		select {
		case <-tous:
		case <-time.After(patience):
			renonce.Store(true)
		}
		return f(v)
	}, &renonce
}

func TestSommeAdditionneLesResultats(t *testing.T) {
	f, _ := simultanes(4, func(v int) int { return v * v })
	if got := Somme([]int{1, 2, 3, 4}, f); got != 30 {
		t.Errorf("Somme = %d, attendu 30", got)
	}
}

func TestSommeAppelleFEnParallele(t *testing.T) {
	f, renonce := simultanes(4, func(v int) int { return v })
	Somme([]int{1, 2, 3, 4}, f)
	if renonce.Load() {
		t.Error("les quatre appels à f n'étaient pas en cours au même moment")
	}
}

func TestSommeAttendTousLesAppels(t *testing.T) {
	var termines atomic.Int32
	lente := func(v int) int {
		time.Sleep(20 * time.Millisecond)
		termines.Add(1)
		return v
	}
	if got := Somme([]int{5, 6, 7}, lente); got != 18 {
		t.Errorf("Somme = %d, attendu 18", got)
	}
	if n := termines.Load(); n != 3 {
		t.Errorf("%d appels terminés au retour de Somme, attendu 3", n)
	}
}

func TestSommeDeRienVautZero(t *testing.T) {
	appels := 0
	if got := Somme(nil, func(v int) int { appels++; return v }); got != 0 || appels != 0 {
		t.Errorf("Somme(nil) = %d avec %d appels, attendu 0 sans appel", got, appels)
	}
}
