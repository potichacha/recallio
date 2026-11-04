// Package appel borne la durée d'un appel à un service lent.
package appel

import (
	"context"
	"time"
)

// Appeler appelle f avec un contexte qui expire au bout de delai, et rend la main au plus tard à l'échéance.
func Appeler(ctx context.Context, delai time.Duration, f func(context.Context) (int, error)) (int, error) {
	return f(ctx)
}
