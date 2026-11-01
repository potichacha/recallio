package com.recallio.group.domain;

import java.time.Instant;
import java.util.UUID;

public interface GroupSettingsRepository {

    /** Les réglages du groupe ; {@link GroupSettings#DEFAULTS} tant que le créateur ne les a pas modifiés. */
    GroupSettings findByGroup(UUID groupId);

    void save(UUID groupId, GroupSettings settings, Instant updatedAt);
}
