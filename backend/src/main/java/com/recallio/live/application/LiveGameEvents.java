package com.recallio.live.application;

import com.recallio.live.domain.LiveGame.Snapshot;

/** Prévient les joueurs connectés qu'une partie a changé (implémenté par le WebSocket, dans la couche api). */
public interface LiveGameEvents {

    /** Diffuse l'état de la partie à toutes ses connexions, sans bloquer. */
    void changed(Snapshot game);
}
