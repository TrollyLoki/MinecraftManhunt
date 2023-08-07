package net.trollyloki.manhunt;

import net.kyori.adventure.text.Component;
import net.trollyloki.manhunt.types.DummyEventHandler;
import net.trollyloki.manhunt.types.ManhuntEventHandler;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ManhuntListener implements Listener {

    private static final DummyEventHandler DUMMY_EVENT_HANDLER = new DummyEventHandler();

    private final ManhuntPlugin plugin;
    private final NamespacedKey targetKey;
    private final HashMap<UUID, AbstractManhunt> manhunts;

    /**
     * Constructs a new manhunt listener
     *
     * @param plugin Plugin
     */
    public ManhuntListener(ManhuntPlugin plugin) {
        this.plugin = plugin;
        this.targetKey = new NamespacedKey(plugin, "target");
        this.manhunts = new HashMap<>();
    }

    private static final String[] WORLD_NAMES = {
            "%s", "%s_nether", "%s_the_end"
    };
    private static final World.Environment[] ENVIRONMENTS = {
            World.Environment.NORMAL, World.Environment.NETHER, World.Environment.THE_END
    };
    /**
     * Creates a new overworld, nether, and end; while causing as little lag as possible
     *
     */
    public CompletableFuture<World[]> createWorlds(String name) {
        final CompletableFuture<World[]> future = new CompletableFuture<>();
        BukkitRunnable runnable = new BukkitRunnable() {

                private final World[] worlds = new World[3];
                private int i = 0;

                @Override
                public void run() {
                    if (i >= 3) {
                        future.complete(worlds);
                        cancel();
                    } else {
                        World world = plugin.getServer().createWorld(
                                new WorldCreator(String.format(WORLD_NAMES[i], name))
                                        .environment(ENVIRONMENTS[i])
                        );
                        world.setKeepSpawnInMemory(false);
                        worlds[i] = world;
                    }

                    i++;
                }

        };
        runnable.runTaskTimer(plugin, 10, 10);
        return future;
    }

    /**
     * Registers a manhunt and creates the associated worlds.
     *
     * @param manhunt Manhunt
     * @return {@code true} if the manhunt was registered
     */
    public CompletableFuture<Boolean> registerManhunt(AbstractManhunt manhunt) {
        if (manhunts.containsValue(manhunt))
            return CompletableFuture.completedFuture(false);
        UUID uuid = UUID.randomUUID();

        return createWorlds(uuid.toString()).thenApply(worlds -> {
            manhunt.setWorlds(worlds);
            manhunts.put(uuid, manhunt);
            return true;
        });
    }

    /**
     * Unregisters a manhunt and unloads the associated worlds.
     *
     * @param manhunt Manhunt
     * @return {@code true} if the manhunt was unregistered
     */
    public boolean unregisterManhunt(AbstractManhunt manhunt) {
        UUID uuid = null;
        for (Map.Entry<UUID, AbstractManhunt> entry : manhunts.entrySet()) {
            if (entry.getValue() == manhunt) {
                uuid = entry.getKey();
                break;
            }
        }

        if (uuid != null) {
            manhunts.remove(uuid);
            Location spawn = plugin.getServer().getWorlds().get(0).getSpawnLocation();
            manhunt.getPlayers().forEach(player -> player.teleport(spawn));
            manhunt.unload();
            return true;
        } else {
            return false;
        }

    }

    /**
     * Gets a manhunt by UUID.
     *
     * @param uuid UUID
     * @return Manhunt
     */
    public AbstractManhunt getManhunt(UUID uuid) {
        return manhunts.get(uuid);
    }

    /**
     * Gets the manhunt for a world.
     *
     * @param world World
     * @return Manhunt
     */
    public AbstractManhunt getManhunt(World world) {
        String name = world.getName();
        if (name.length() < 36)
            return null;
        int i = name.indexOf('_');
        if (i < 0)
            i = name.length();
        return getManhunt(UUID.fromString(name.substring(0, i)));
    }

    /**
     * Gets the event handler for a world.
     *
     * @param world World
     * @return Event handler
     */
    public ManhuntEventHandler getEventHandler(World world) {
        AbstractManhunt manhunt = getManhunt(world);
        return manhunt != null ? manhunt : DUMMY_EVENT_HANDLER;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        getEventHandler(event.getEntity().getWorld()).onEntityDamage(event);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        getEventHandler(event.getEntity().getWorld()).onEntityDamageByEntity(event);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        getEventHandler(event.getEntity().getWorld()).onEntityDeath(event);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        getEventHandler(event.getEntity().getWorld()).onPlayerDeath(event);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        getEventHandler(event.getPlayer().getWorld()).onPlayerRespawn(event);
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        getEventHandler(event.getPlayer().getWorld()).onPlayerPortal(event);
    }

    @EventHandler
    public void onEntityPortal(EntityPortalEvent event) {
        getEventHandler(event.getEntity().getWorld()).onEntityPortal(event);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        getEventHandler(event.getPlayer().getWorld()).onPlayerJoin(event);
    }

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        getEventHandler(event.getPlayer().getWorld()).onPlayerAdvancementDone(event);
    }

}
