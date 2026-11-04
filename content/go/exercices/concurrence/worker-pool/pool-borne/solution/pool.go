// Package pool applique une fonction à des tâches avec un nombre borné de workers.
package pool

import "sync"

// Traiter applique f à chaque tâche avec au plus nWorkers appels simultanés, et renvoie les résultats dans l'ordre.
func Traiter(taches []int, nWorkers int, f func(int) int) []int {
	nWorkers = max(nWorkers, 1)
	resultats := make([]int, len(taches))
	jobs := make(chan int)
	var wg sync.WaitGroup
	for range nWorkers {
		wg.Go(func() {
			for i := range jobs {
				resultats[i] = f(taches[i])
			}
		})
	}
	for i := range taches {
		jobs <- i
	}
	close(jobs)
	wg.Wait()
	return resultats
}
