package com.recallio.user.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUserEntity {

    @Id
    UUID id;

    @Column(name = "oidc_subject", nullable = false, unique = true)
    String oidcSubject;

    @Column(name = "display_name", nullable = false)
    String displayName;

    @Column(nullable = false)
    String timezone;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected AppUserEntity() {}
}
