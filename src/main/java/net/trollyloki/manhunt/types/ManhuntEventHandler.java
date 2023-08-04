package net.trollyloki.manhunt.types;

import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public interface ManhuntEventHandler {

    void onEntityDamage(EntityDamageEvent event);

    void onEntityDamageByEntity(EntityDamageByEntityEvent event);

    void onEntityDeath(EntityDeathEvent event);

    void onPlayerDeath(PlayerDeathEvent event);

    void onPlayerRespawn(PlayerRespawnEvent event);

    void onPlayerPortal(PlayerPortalEvent event);

    void onPlayerJoin(PlayerJoinEvent event);

    void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event);

    void onEntityPortal(EntityPortalEvent event);
}
