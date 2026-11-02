package com.recallio.live.infrastructure;

import com.recallio.live.domain.LiveGame;
import com.recallio.live.domain.LiveGameRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Les parties ouvertes, dans la mémoire de l'instance : elles ne survivent pas à un redémarrage (ADR 0021). */
@ApplicationScoped
public class InMemoryLiveGameRepository implements LiveGameRepository {

    private final Map<UUID, LiveGame> games = new ConcurrentHashMap<>();
    /** La partie ouverte de chaque groupe ; {@code computeIfAbsent} rend l'enregistrement atomique. */
    private final Map<UUID, UUID> openGameByGroup = new ConcurrentHashMap<>();

    @Override
    public Optional<LiveGame> registerUnlessGroupHasOne(LiveGame game) {
        UUID openId = openGameByGroup.computeIfAbsent(game.groupId(), _ -> {
            games.put(game.id(), game);
            return game.id();
        });
        return openId.equals(game.id()) ? Optional.empty() : Optional.ofNullable(games.get(openId));
    }

    @Override
    public Optional<LiveGame> findById(UUID gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    @Override
    public Optional<LiveGame> findOpenByGroup(UUID groupId) {
        return Optional.ofNullable(openGameByGroup.get(groupId)).map(games::get);
    }

    @Override
    public int openCount() {
        return games.size();
    }

    @Override
    public void remove(LiveGame game) {
        games.remove(game.id());
        openGameByGroup.remove(game.groupId(), game.id());
    }
}
