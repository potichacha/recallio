package com.recallio.live.api;

import com.recallio.live.api.LiveDtos.LiveEventDto;
import com.recallio.live.application.LiveGameService;
import com.recallio.live.domain.LiveGame.Snapshot;
import com.recallio.platform.domain.ConflictException;
import com.recallio.platform.domain.DomainException;
import com.recallio.platform.domain.ResourceNotFoundException;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.UserData.TypedKey;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Le direct d'une partie (ADR 0021). Chaque connexion rejoint la partie ; à chaque arrivée ou départ, l'état de la
 * partie est diffusé à toutes ses connexions. L'ouverture est authentifiée par le même JWT que l'API REST, transmis par
 * le sous-protocole ; un refus ferme la connexion avec un code applicatif (4404, 4409, 4422).
 */
@WebSocket(path = "/api/v1/live/{gameId}")
@Authenticated
public class LiveGameSocket {

    private static final Logger LOG = Logger.getLogger(LiveGameSocket.class);

    /** Codes de fermeture applicatifs (plage 4000-4999 de la RFC 6455), calqués sur les statuts HTTP. */
    static final int CLOSE_NOT_FOUND = 4404;

    static final int CLOSE_CONFLICT = 4409;
    static final int CLOSE_REFUSED = 4422;

    /** L'utilisateur de la connexion, gardé pour son départ : à la fermeture, la requête d'ouverture est loin. */
    private static final TypedKey<UUID> USER_ID = new TypedKey<>("userId");

    private final LiveGameService games;
    private final CurrentUser currentUser;
    private final WebSocketConnection connection;
    private final OpenConnections openConnections;

    public LiveGameSocket(
            LiveGameService games,
            CurrentUser currentUser,
            WebSocketConnection connection,
            OpenConnections openConnections) {
        this.games = games;
        this.currentUser = currentUser;
        this.connection = connection;
        this.openConnections = openConnections;
    }

    @OnOpen
    public void onOpen() {
        try {
            var user = currentUser.get();
            Snapshot game = games.join(user, gameId(connection));
            connection.userData().put(USER_ID, user.id());
            broadcast(game);
        } catch (DomainException | IllegalArgumentException e) {
            connection.closeAndAwait(new CloseReason(closeCode(e), e.getMessage()));
        }
    }

    @OnClose
    public void onClose() {
        UUID userId = connection.userData().get(USER_ID);
        if (userId != null) {
            games.leave(userId, gameId(connection)).ifPresent(this::broadcast);
        }
    }

    /** L'état de la partie, à chaque connexion ouverte sur elle. */
    private void broadcast(Snapshot game) {
        LiveEventDto event = LiveEventDto.game(game);
        openConnections.findByEndpointId(connection.endpointId()).stream()
                .filter(other -> other.isOpen() && game.id().toString().equals(other.pathParam("gameId")))
                .forEach(other -> other.sendText(event)
                        .subscribe()
                        .with(_ -> {}, failure -> LOG.debugf("Diffusion impossible sur %s", other.id())));
    }

    private static UUID gameId(WebSocketConnection connection) {
        return UUID.fromString(connection.pathParam("gameId"));
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
