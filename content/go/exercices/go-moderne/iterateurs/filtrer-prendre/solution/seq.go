// Package seq transforme des itérateurs de façon paresseuse.
package seq

import "iter"

// Filtrer renvoie les éléments de seq pour lesquels garder renvoie true.
func Filtrer[T any](seq iter.Seq[T], garder func(T) bool) iter.Seq[T] {
	return func(yield func(T) bool) {
		for v := range seq {
			if garder(v) && !yield(v) {
				return
			}
		}
	}
}

// Prendre renvoie au plus les n premiers éléments de seq.
func Prendre[T any](seq iter.Seq[T], n int) iter.Seq[T] {
	return func(yield func(T) bool) {
		if n <= 0 {
			return
		}
		donnes := 0
		for v := range seq {
			if !yield(v) {
				return
			}
			donnes++
			if donnes == n {
				return
			}
		}
	}
}
