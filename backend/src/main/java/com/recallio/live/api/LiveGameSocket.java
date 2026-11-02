package com.recallio.live.api;

import com.recallio.live.application.LiveGameService;
import com.recallio.platform.domain.ConflictException;
import com.recallio.platform.domain.DomainException;
import com.recallio.platform.domain.ResourceNotFoundException;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.UserData.TypedKey;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import java.util.UUID;

/**
 * Le direct d'une partie (ADR 0021). Chaque connexion rejoint la partie et la quitte en se fermant ; à chaque
 * changement, {@link LiveGameBroadcaster} diffuse l'état de la partie à toutes ses connexions. Les commandes (démarrer,
 * répondre) passent par l'API REST ({@link LiveGamesResource}) : le WebSocket ne sert qu'à recevoir.
 *
 * <p>L'ouverture est authentifiée par le même JWT que l'API REST, transmis par le sous-protocole ; un refus ferme la
 * connexion avec un code applicatif (4404, 4409, 4422).
 */
@WebSocket(path = "/api/v1/live/{gameId}", endpointId = LiveGameSocket.ENDPOINT_ID)
@Authenticated
public class LiveGameSocket {

    static final String ENDPOINT_ID = "live-game";

    /** Codes de fermeture applicatifs (plage 4000-4999 de la RFC 6455), calqués sur les statuts HTTP. */
    static final int CLOSE_NOT_FOUND = 4404;

    static final int CLOSE_CONFLICT = 4409;
    static final int CLOSE_REFUSED = 4422;

    /** L'utilisateur de la connexion, gardé pour son départ : à la fermeture, la requête d'ouverture est loin. */
    private static final TypedKey<UUID> USER_ID = new TypedKey<>("userId");

    private final LiveGameService games;
    private final CurrentUser currentUser;
    private final WebSocketConnection connection;

    public LiveGameSocket(LiveGameService games, CurrentUser currentUser, WebSocketConnection connection) {
        this.games = games;
        this.currentUser = currentUser;
        this.connection = connection;
    }

    @OnOpen
    public void onOpen() {
        try {
            var user = currentUser.get();
            UUID gameId = UUID.fromString(connection.pathParam("gameId"));
            // Retenu avant de rejoindre : la diffusion qui suit inclut déjà cette connexion.
            connection.userData().put(USER_ID, user.id());
            games.join(user, gameId);
        } catch (DomainException | IllegalArgumentException e) {
            connection.userData().remove(USER_ID);
            connection.closeAndAwait(new CloseReason(closeCode(e), e.getMessage()));
        }
    }

    @OnClose
    public void onClose() {
        UUID userId = connection.userData().get(USER_ID);
        if (userId != null) {
            games.leave(userId, UUID.fromString(connection.pathParam("gameId")));
        }
    }

    private static int closeCode(RuntimeException e) {
        return switch (e) {
            case ResourceNotFoundException _ -> CLOSE_NOT_FOUND;
            case ConflictException _ -> CLOSE_CONFLICT;
            case IllegalArgumentException _ -> CLOSE_NOT_FOUND; // identifiant de partie mal formé
            default -> CLOSE_REFUSED;
        };
    }
}
