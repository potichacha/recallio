package pool

import (
	"reflect"
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

const patience = 500 * time.Millisecond

// compteur mesure le nombre maximal d'appels simultanés.
type compteur struct {
	enCours, max atomic.Int32
}

func (c *compteur) entrer() {
	n := c.enCours.Add(1)
	for {
		m := c.max.Load()
		if n <= m || c.max.CompareAndSwap(m, n) {
			return
		}
	}
}

func (c *compteur) sortir() { c.enCours.Add(-1) }

func TestTraiterRangeLesResultatsDansLOrdre(t *testing.T) {
	got := Traiter([]int{1, 2, 3, 4, 5, 6, 7}, 3, func(v int) int { return v * 10 })
	want := []int{10, 20, 30, 40, 50, 60, 70}
	if !reflect.DeepEqual(got, want) {
		t.Errorf("Traiter = %v, attendu %v", got, want)
	}
}

func TestTraiterNeDepassePasLeNombreDeWorkers(t *testing.T) {
	var c compteur
	Traiter(make([]int, 30), 3, func(v int) int {
		c.entrer()
		defer c.sortir()
		time.Sleep(2 * time.Millisecond)
		return v
	})
	if m := c.max.Load(); m > 3 {
		t.Errorf("%d appels simultanés, attendu au plus 3", m)
	}
}

func TestTraiterUtiliseTousLesWorkers(t *testing.T) {
	const n = 3
	var demarres sync.WaitGroup
	demarres.Add(n)
	tous := make(chan struct{})
	go func() {
		demarres.Wait()
		close(tous)
	}()
	var appels atomic.Int32
	var renonce atomic.Bool
	Traiter(make([]int, 9), n, func(v int) int {
		if appels.Add(1) <= n {
			demarres.Done()
			select {
			case <-tous:
			case <-time.After(patience):
				renonce.Store(true)
			}
		}
		return v
	})
	if renonce.Load() {
		t.Errorf("les %d premiers appels n'étaient pas en cours au même moment", n)
	}
}

func TestTraiterAvecZeroWorkerEnUtiliseUn(t *testing.T) {
	var c compteur
	got := Traiter([]int{1, 2, 3}, 0, func(v int) int {
		c.entrer()
		defer c.sortir()
		return v + 1
	})
	if !reflect.DeepEqual(got, []int{2, 3, 4}) || c.max.Load() != 1 {
		t.Errorf("Traiter(0 worker) = %v avec %d appels simultanés, attendu [2 3 4] avec 1", got, c.max.Load())
	}
}

func TestTraiterSansTache(t *testing.T) {
	if got := Traiter(nil, 4, func(v int) int { return v }); len(got) != 0 {
		t.Errorf("Traiter(nil) = %v, attendu vide", got)
	}
}
