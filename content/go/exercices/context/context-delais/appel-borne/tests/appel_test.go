package appel

import (
	"context"
	"errors"
	"testing"
	"time"
)

const (
	court = 50 * time.Millisecond
	long  = time.Second
)

func TestAppelerRenvoieLeResultatDUnAppelRapide(t *testing.T) {
	v, err := Appeler(context.Background(), long, func(context.Context) (int, error) { return 42, nil })
	if v != 42 || err != nil {
		t.Errorf("Appeler = %d, %v ; attendu 42, nil", v, err)
	}
}

func TestAppelerRenvoieLErreurDeF(t *testing.T) {
	panne := errors.New("panne")
	_, err := Appeler(context.Background(), long, func(context.Context) (int, error) { return 0, panne })
	if !errors.Is(err, panne) {
		t.Errorf("err = %v, attendu %v", err, panne)
	}
}

func TestFRecoitUnContexteAvecEcheance(t *testing.T) {
	var aEcheance bool
	Appeler(context.Background(), long, func(ctx context.Context) (int, error) {
		_, aEcheance = ctx.Deadline()
		return 0, nil
	})
	if !aEcheance {
		t.Error("le contexte passé à f n'a pas d'échéance")
	}
}

func TestAppelerRendLaMainALEcheanceMemeSiFIgnoreLeContexte(t *testing.T) {
	debut := time.Now()
	v, err := Appeler(context.Background(), court, func(context.Context) (int, error) {
		time.Sleep(long) // ignore son contexte
		return 1, nil
	})
	if duree := time.Since(debut); duree > long/2 {
		t.Errorf("Appeler a mis %v, attendu environ %v", duree, court)
	}
	if v != 0 || !errors.Is(err, context.DeadlineExceeded) {
		t.Errorf("Appeler = %d, %v ; attendu 0, DeadlineExceeded", v, err)
	}
}

func TestAppelerRendLaMainQuandLeParentEstAnnule(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	cancel()
	_, err := Appeler(ctx, long, func(context.Context) (int, error) {
		time.Sleep(long) // ignore son contexte
		return 1, nil
	})
	if !errors.Is(err, context.Canceled) {
		t.Errorf("err = %v, attendu context.Canceled", err)
	}
}

func TestLeContexteDeFEstAnnuleApresLeRetour(t *testing.T) {
	var recu context.Context
	Appeler(context.Background(), long, func(ctx context.Context) (int, error) {
		recu = ctx
		return 0, nil
	})
	if recu.Err() == nil {
		t.Error("le contexte de f est toujours actif après le retour : cancel n'a pas été appelé")
	}
}
