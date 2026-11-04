// Package appel borne la durée d'un appel à un service lent.
package appel

import (
	"context"
	"time"
)

type resultat struct {
	valeur int
	err    error
}

// Appeler appelle f avec un contexte qui expire au bout de delai, et rend la main au plus tard à l'échéance.
func Appeler(ctx context.Context, delai time.Duration, f func(context.Context) (int, error)) (int, error) {
	ctx, cancel := context.WithTimeout(ctx, delai)
	defer cancel()

	reponse := make(chan resultat, 1) // la goroutine peut répondre même si l'on est déjà parti
	go func() {
		v, err := f(ctx)
		reponse <- resultat{v, err}
	}()

	select {
	case r := <-reponse:
		return r.valeur, r.err
	case <-ctx.Done():
		return 0, ctx.Err()
	}
}
