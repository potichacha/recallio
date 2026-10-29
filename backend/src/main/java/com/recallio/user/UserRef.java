package com.recallio.user;

import java.time.ZoneId;
import java.util.UUID;

/**
 * Ce que les autres modules savent d'un utilisateur : son identifiant applicatif et son fuseau horaire, qui définit
 * « le jour » (nouvelles cartes quotidiennes, streak).
 */
public record UserRef(UUID id, ZoneId timezone) {}
