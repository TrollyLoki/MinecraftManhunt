package net.trollyloki.manhunt.types;

import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class DummyEventHandler implements ManhuntEventHandler {

    @Override
    public void onEntityDamage(EntityDamageEvent event) {

    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

    }

    @Override
    public void onEntityDeath(EntityDeathEvent event) {

    }

    @Override
    public void onPlayerDeath(PlayerDeathEvent event) {

    }

    @Override
    public void onPlayerRespawn(PlayerRespawnEvent event) {

    }

    @Override
    public void onPlayerPortal(PlayerPortalEvent event) {

    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent event) {

    }

    @Override
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {

    }

    @Override
    public void onEntityPortal(EntityPortalEvent event) {

    }

}
