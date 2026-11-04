// Package copie recopie un flux en refusant ce qui dépasse une taille maximale.
package copie

import (
	"errors"
	"io"
)

// ErrTropGros signale une source plus grande que la limite.
var ErrTropGros = errors.New("source trop volumineuse")

// Copier recopie au plus limite octets de src dans dst, et renvoie ErrTropGros si src en contient davantage.
func Copier(dst io.Writer, src io.Reader, limite int64) (int64, error) {
	n, err := io.Copy(dst, io.LimitReader(src, limite))
	if err != nil {
		return n, err
	}
	encore, err := io.CopyN(io.Discard, src, 1)
	if encore > 0 {
		return n, ErrTropGros
	}
	if err != nil && err != io.EOF {
		return n, err
	}
	return n, nil
}
