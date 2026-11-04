package duree

import (
	"fmt"
	"testing"
)

func TestStringAfficheHeuresMinutesEtSecondes(t *testing.T) {
	cas := map[Duree]string{3725: "1h02m05s", 3600: "1h00m00s", 65: "1m05s", 5: "5s", 0: "0s", -65: "-1m05s"}
	for d, attendu := range cas {
		if got := d.String(); got != attendu {
			t.Errorf("Duree(%d).String() = %q, attendu %q", int(d), got, attendu)
		}
	}
}

func TestFmtUtiliseString(t *testing.T) {
	if got := fmt.Sprint(Duree(90)); got != "1m30s" {
		t.Errorf("fmt.Sprint(Duree(90)) = %q, attendu \"1m30s\"", got)
	}
}

func TestAjouterRenvoieLaSommeSansModifierLeReceveur(t *testing.T) {
	d := Duree(60)
	if got := d.Ajouter(30); got != 90 {
		t.Errorf("Ajouter = %d, attendu 90", int(got))
	}
	if d != 60 {
		t.Errorf("le receveur a changé : %d", int(d))
	}
}
