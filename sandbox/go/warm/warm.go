// Package warm compile, à la construction de l'image, les paquets standard dont se servent les exercices : ils entrent
// dans le cache de compilation, et une exécution ne recompile plus que le code du joueur (ADR 0033). Un exercice peut
// importer un autre paquet standard : il sera compilé à chaque exécution, donc plus lentement.
package warm

import (
	_ "bufio"
	_ "bytes"
	_ "cmp"
	_ "container/heap"
	_ "container/list"
	_ "context"
	_ "encoding/csv"
	_ "encoding/json"
	_ "errors"
	_ "fmt"
	_ "io"
	_ "iter"
	_ "log/slog"
	_ "maps"
	_ "math"
	_ "math/rand/v2"
	_ "net/http"
	_ "net/http/httptest"
	_ "os"
	_ "regexp"
	_ "slices"
	_ "sort"
	_ "strconv"
	_ "strings"
	_ "sync"
	_ "sync/atomic"
	_ "time"
	_ "unicode"
	_ "unicode/utf8"
)
