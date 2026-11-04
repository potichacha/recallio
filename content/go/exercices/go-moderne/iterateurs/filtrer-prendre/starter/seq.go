// Package seq transforme des itérateurs de façon paresseuse.
package seq

import "iter"

// Filtrer renvoie les éléments de seq pour lesquels garder renvoie true.
func Filtrer[T any](seq iter.Seq[T], garder func(T) bool) iter.Seq[T] {
	return func(yield func(T) bool) {}
}

// Prendre renvoie au plus les n premiers éléments de seq.
func Prendre[T any](seq iter.Seq[T], n int) iter.Seq[T] {
	return func(yield func(T) bool) {}
}
