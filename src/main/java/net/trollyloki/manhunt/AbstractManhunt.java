package net.trollyloki.manhunt;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.trollyloki.manhunt.DropHolder;
import net.trollyloki.manhunt.ManhuntPlugin;
import net.trollyloki.manhunt.Utils;
import net.trollyloki.manhunt.compass.CompassListener;
import net.trollyloki.manhunt.compass.PlayerCompassTarget;
import net.trollyloki.manhunt.types.ManhuntEventHandler;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Represents a manhunt
 */
public class AbstractManhunt extends BukkitRunnable implements ManhuntEventHandler {

    private World overworld, nether, end;
    private final ManhuntPlugin plugin;
    private final Set<UUID> runners, hunters;

    /**
     * Constructs a new manhunt
     *
     * @param plugin Plugin
     */
    public AbstractManhunt(ManhuntPlugin plugin) {
        this.plugin = plugin;
        this.runners = new HashSet<>();
        this.hunters = new HashSet<>();
        runTaskTimer(plugin, 0, 0);
    }

    /**
     * Sets the worlds for this manhunt.
     *
     * @param worlds [Overworld, Nether, End]
     */
    void setWorlds(World[] worlds) {
        this.overworld = worlds[0];
        this.nether = worlds[1];
        this.end = worlds[2];
    }

    public void unload() {
        plugin.getServer().unloadWorld(overworld, true);
        plugin.getServer().unloadWorld(nether, true);
        plugin.getServer().unloadWorld(end, true);
        overworld = null;
        nether = null;
        end = null;
    }

    public ManhuntPlugin getPlugin() {
        return plugin;
    }

    /**
     * Gets all players in this manhunt
     *
     * @return Set of players
     */
    public Set<Player> getPlayers() {
        HashSet<UUID> uuids = new HashSet<>();
        uuids.addAll(runners);
        uuids.addAll(hunters);
        return getPlayers(uuids);
    }

    /**
     * Converts a set of UUIDs to a set of players
     *
     * @param uuids Set of UUIDs
     * @return Set of players
     */
    private Set<Player> getPlayers(Set<UUID> uuids) {
        HashSet<Player> players = new HashSet<>();
        for (UUID uuid : uuids) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null)
                players.add(player);
        }
        return players;
    }

    /**
     * Gets all runners in this manhunt
     *
     * @return Set of players
     */
    public Set<Player> getRunners() {
        return getPlayers(runners);
    }

    /**
     * Adds a runner to this manhunt
     *
     * @param runner Runner
     * @return {@code true} if the runner was added
     */
    public boolean addRunner(UUID runner) {
        hunters.remove(runner); // remove if hunter
        return runners.add(runner);
    }

    /**
     * Checks if the given player is a runner in this manhunt
     *
     * @param player Player
     * @return {@code true} if the player is a runner
     */
    public boolean isRunner(UUID player) {
        return runners.contains(player);
    }

    /**
     * Gets all hunters in this manhunt
     *
     * @return Set of players
     */
    public Set<Player> getHunters() {
        return getPlayers(hunters);
    }

    /**
     * Adds a hunter to this manhunt
     *
     * @param hunter Hunter
     * @return {@code true} if the hunter was added
     */
    public boolean addHunter(UUID hunter) {
        runners.remove(hunter); // remove if runner
        return hunters.add(hunter);
    }

    /**
     * Checks if the given player is a hunter in this manhunt
     *
     * @param player Player
     * @return {@code true} if the player is a hunter
     */
    public boolean isHunter(UUID player) {
        return hunters.contains(player);
    }

    /**
     * Checks if the given player is in this manhunt
     *
     * @param player Player
     * @return {@code true} if the player is in this manhunt
     */
    public boolean contains(UUID player) {
        return runners.contains(player) || hunters.contains(player);
    }

    /**
     * Removes the given player from this manhunt
     *
     * @param player Player
     * @return {@code true} if the player was removed
     */
    public boolean remove(UUID player) {
        return runners.remove(player) || hunters.remove(player);
    }

    /**
     * Broadcasts a message to all online players
     */
    public void broadcast(String message) {
        for (Player player : plugin.getServer().getOnlinePlayers())
            player.sendMessage(message);
    }

    /**
     * Gives the given player all available tracking compasses
     *
     * @param player Player
     * @return Amount of tracking compasses that were given to the player
     */
    public int giveTrackingCompasses(Player player) {
        if (isHunter(player.getUniqueId())) {
            int count = 0;
            for (UUID runner : runners) {
                ItemStack compass = CompassListener.createCompassItem(new PlayerCompassTarget(runner));
                CompassListener.updateCompass(compass, player.getLocation());
                if (!player.getInventory().addItem(compass).isEmpty())
                    player.getWorld().dropItem(player.getLocation(), compass);
                count++;
            }
            return count;
        }
        return 0;
    }

    private boolean running = false, paused = false, won = false;
    private int runnersEliminated = 0;
    private int time;
    private int netherEntryTime = -1;
    private Location runnerSpawnLocation, hunterSpawnLocation;
    private Map<UUID, Integer> lastHits;
    private Map<UUID, DropHolder> drops;
    private Map<UUID, Boolean> canRevive;
    private Map<UUID, Integer> revives;
    private Map<UUID, Integer> invincibility;

    /**
     * Checks if this manhunt is running
     *
     * @return {@code true} if this manhunt is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Gets the amount of time that this manhunt has been running
     *
     * @return Number of ticks
     */
    public int getTime() {
        return time;
    }

    /**
     * Gets the amount of time remaining in the grace period
     *
     * @return Number of ticks
     */
    public int getGraceRemaining() {
        return plugin.getConfig().getInt("game.combat-grace") * 20 - getTime();
    }

    /**
     * Checks if the grace period is in effect
     *
     * @return {@code true} if the grace period is in effect
     */
    public boolean isGracePeriod() {
        return getGraceRemaining() >= 0;
    }

    /**
     * Gets the amount of time remaining in the nether grace period
     *
     * @return Number of ticks
     */
    public int getNetherGraceRemaining() {
        int grace = plugin.getConfig().getInt("game.nether-grace") * 20;
        if (netherEntryTime < 0)
            return grace;
        return grace - (getTime() - netherEntryTime);
    }

    /**
     * Checks if the nether grace period is in effect
     *
     * @return {@code true} if the nether grace period is in effect
     */
    public boolean isNetherGracePeriod() {
        return getNetherGraceRemaining() >= 0;
    }

    /**
     * Sets the nether entry time to the current time if it is not already set
     *
     * @return {@code true} if the entry time was set
     */
    public boolean setNetherEntryTime() {
        if (netherEntryTime < 0) {
            netherEntryTime = time;
            return true;
        }
        return false;
    }

    /**
     * Gets the spawn location for the given player
     *
     * @param player Player
     * @return Spawn location
     */
    public Location getSpawnLocation(UUID player) {
        if (isRunner(player))
            return runnerSpawnLocation;
        else if (isHunter(player))
            return hunterSpawnLocation;
        else
            return null;
    }

    /**
     * Gets the number of revives the given player has left
     *
     * @param player Player
     * @return Number of revives
     */
    public Integer getRevives(UUID player) {
        if (!isRunning())
            return null;
        return revives.get(player);
    }

    /**
     * Revives the given player
     *
     * @param player Player
     * @return Number of revives remaining
     */
    public Integer revive(Player player) {
        if (!isRunning())
            return null;

        DropHolder dropHolder = drops.get(player.getUniqueId());
        if (dropHolder != null) {
            invincibility.put(player.getUniqueId(), getTime() + plugin.getConfig().getInt("game.invincibility") * 20);
            dropHolder.give(player);

            int revives = getRevives(player.getUniqueId());
            if (revives > 0) {
                this.revives.put(player.getUniqueId(), --revives);
                Score score = plugin.getServer().getScoreboardManager().getMainScoreboard().getObjective("revives").getScore(player.getName());
                score.setScore(score.getScore() - 1);
            }
            return revives;
        }

        return null;
    }

    /**
     * Drops the given player's item where they died
     *
     * @param player Player
     */
    public void dropItems(Player player) {

        DropHolder dropHolder = drops.get(player.getUniqueId());
        if (dropHolder != null) {
            dropHolder.drop();
        }

    }

    /**
     * Gets the invincibility time the given player has left
     *
     * @param player Player
     * @return Ticks of invincibility remaining
     */
    public int getInvincibility(UUID player) {
        Integer invincibilityTime = invincibility.get(player);
        if (invincibilityTime != null)
            return invincibilityTime - getTime();
        return -1;
    }

    /**
     * Sets the given player into spectator mode
     *
     * @param player Player
     */
    public void spectate(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 1000000, 0, true, false, false));
    }

    /**
     * Starts this manhunt
     *
     * @return {@code true} if this manhunt was started
     */
    public boolean start() {
        return start(s -> {});
    }

    /**
     * Starts this manhunt
     *
     * @param output Consumer to output messages to
     * @return {@code true} if this manhunt was started
     */
    public boolean start(Consumer<String> output) {
        if (isRunning())
            return false;
        if (overworld == null || nether == null || end == null)
            throw new IllegalStateException("One or more worlds are not ready");

        Scoreboard scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();

        Team runnerTeam = scoreboard.getTeam("runners");
        if (runnerTeam == null)
            runnerTeam = scoreboard.registerNewTeam("runners");
        runnerTeam.setColor(ChatColor.valueOf(plugin.getConfigString("scoreboard.runner-color")));

        Team hunterTeam = scoreboard.getTeam("hunters");
        if (hunterTeam == null)
            hunterTeam = scoreboard.registerNewTeam("hunters");
        hunterTeam.setColor(ChatColor.valueOf(plugin.getConfigString("scoreboard.hunter-color")));

        Objective health = scoreboard.getObjective("health");
        if (health == null)
            health = scoreboard.registerNewObjective("health", "health", "Health");
        health.setRenderType(RenderType.HEARTS);
        health.setDisplaySlot(DisplaySlot.PLAYER_LIST);

        Objective revivesObj = scoreboard.getObjective("revives");
        if (revivesObj != null) revivesObj.unregister();
        revivesObj = scoreboard.registerNewObjective("revives", "dummy", "Revives Remaining");
        revivesObj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int maxDistance = plugin.getConfig().getInt("game.max-distance");
        output.accept("Choosing safe spawn locations...");
        CompletableFuture<Location> runnerSpawnLocationFuture = Utils.getRandomSafeLocation(plugin, overworld, maxDistance);
        CompletableFuture<Location> hunterSpawnLocationFuture = Utils.getRandomSafeLocation(plugin, overworld, maxDistance);

        // Lambda Pain
        Team finalRunnerTeam = runnerTeam;
        Team finalHunterTeam = hunterTeam;
        Objective finalHealth = health;
        Objective finalRevives = revivesObj;

        runnerSpawnLocationFuture.thenAcceptBoth(hunterSpawnLocationFuture, (rl, hl) -> {

            runnerSpawnLocation = rl;
            hunterSpawnLocation = hl;
            output.accept("Spawn locations found!");

            int worldborderSize = plugin.getConfig().getInt("game.worldborder-radius") * 2;
            overworld.getWorldBorder().setSize(worldborderSize);
            nether.getWorldBorder().setSize(worldborderSize / 8);
            end.getWorldBorder().setSize(worldborderSize);

            lastHits = new HashMap<>();
            drops = new HashMap<>();
            canRevive = new HashMap<>();
            revives = new HashMap<>();
            invincibility = new HashMap<>();

            output.accept("Setting up players...");
            int pearls = getStartingPearlCount();
            int runners = this.runners.size();
            int pearlsRemainder = 0;
            if (runners != 0) {
                pearlsRemainder = pearls % runners;
                pearls /= runners;
            }

            int reviveCount = plugin.getConfig().getInt("game.revives");

            Iterator<Advancement> advancementIterator = plugin.getServer().advancementIterator();
            for (Player player : plugin.getServer().getOnlinePlayers()) {

                Team team = null;
                Location location = null;
                if (isRunner(player.getUniqueId())) {
                    team = finalRunnerTeam;
                    location = runnerSpawnLocation;
                } else if (isHunter(player.getUniqueId())) {
                    team = finalHunterTeam;
                    location = hunterSpawnLocation;
                }

                if (team != null) {
                    team.addEntry(player.getName());
                    //finalHealth.getScore(player.getName()).setScore(20);
                    finalRevives.getScore(player.getName()).setScore(reviveCount);
                    revives.put(player.getUniqueId(), reviveCount);

                    player.spigot().respawn();
                    player.teleport(location);
                    player.setGameMode(GameMode.SURVIVAL);
                    player.getInventory().clear();
                    player.setTotalExperience(0);
                    Utils.clearAdvancements(player, advancementIterator);
                    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    player.setHealth(20);
                    player.setFoodLevel(20);
                    player.setSaturation(5);
                    giveTrackingCompasses(player);

                    if (team == finalRunnerTeam) {
                        int pearlCount = pearls;
                        if (pearlsRemainder > 0) {
                            pearlsRemainder--;
                            pearlCount++;
                        }
                        if (pearlCount > 0) {
                            player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, pearlCount));
                        }
                    }
                }

                else {
                    spectate(player);
                }

            }

            time = 0;
            netherEntryTime = -1;
            overworld.setTime(0);
            nether.setTime(0);
            end.setTime(0);
            running = true;
            output.accept("Complete!");

        }).exceptionally(e -> {
            output.accept("ERROR: " + e);
            e.printStackTrace();
            return null;
        });
        return true;
    }

    protected int getStartingPearlCount() {
        return 0;
    }

    /**
     * Sets this manhunt as paused or unpaused
     *
     * @param paused Paused
     */
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /**
     * Checks if this manhunt is paused
     *
     * @return {@code true} if this manhunt is paused
     */
    public boolean isPaused() {
        return this.paused;
    }

    /**
     * Marks this manhunt as won and pauses the timer
     *
     * @param team Winning team, either {@code "hunters"} or {@code "runners"}
     * @return {@code true} if this manhunt was not already won
     */
    public boolean win(String team) {
        if (won)
            return false;
        won = true;
        setPaused(true);
        broadcast(plugin.getConfigString("game." + team + "-win"));
        return true;
    }

    /**
     * Stops this manhunt
     *
     * @return {@code true} if this manhunt was stopped
     */
    public boolean stop() {
        if (!isRunning())
            return false;

        running = false;

        return true;
    }

    @Override
    public void run() {
        if (!isRunning())
            return;

        int grace = getGraceRemaining();
        String graceWarning = null;
        if (grace % 20 == 0) {
            graceWarning = plugin.getConfigString("game.grace-warnings." + grace / 20);
            if (graceWarning != null)
                graceWarning = plugin.getConfigString("game.combat-grace-warning") + graceWarning;
        }

        int netherGrace = getNetherGraceRemaining();
        String netherGraceWarning = null;
        if (netherEntryTime >= 0 && netherGrace % 20 == 0) {
            netherGraceWarning = plugin.getConfigString("game.grace-warnings." + netherGrace / 20);
            if (netherGraceWarning != null)
                netherGraceWarning = plugin.getConfigString("game.nether-grace-warning") + netherGraceWarning;
        }

        String actionbar = null;
        String title = null;
        if (time % 1 == 0) {
            String time = Utils.formatTime(getTime());
            String graceTime = Utils.formatTime(Math.max(grace, 0));
            String netherGraceTime = Utils.formatTime(Math.max(netherGrace, 0));
            actionbar = String.format(plugin.getConfigString("game.actionbar"),
                    time, graceTime, netherGraceTime);
            if (isPaused() && !won)
                title = plugin.getConfigString("game.pause-title");
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {

            if (!isPaused() && graceWarning != null)
                player.sendMessage(graceWarning);
            if (!isPaused() && netherGraceWarning != null)
                player.sendMessage(netherGraceWarning);
            if (actionbar != null)
                player.sendActionBar(Component.text(actionbar));
            if (title != null)
                player.sendTitle(title, null, 0, 5, 0);

            int invincibilityTime = getInvincibility(player.getUniqueId());
            if (!isPaused() && invincibilityTime >= 0 && invincibilityTime % 20 == 0) {
                String invincibilityWarning = plugin.getConfigString("game.invincibility-warnings." + invincibilityTime / 20);
                if (invincibilityWarning != null) {
                    invincibilityWarning = plugin.getConfigString("game.invincibility-warning") + invincibilityWarning;
                    player.sendMessage(invincibilityWarning);
                }
            }

        }

        if (!isPaused())
            time++;
    }

    @Override
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (isRunning() && contains(player.getUniqueId())) {

                if (getInvincibility(event.getEntity().getUniqueId()) >= 0)
                    event.setCancelled(true);

            }
        }
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player player = (Player) event.getEntity();
            Player damager = (Player) event.getDamager();

            if (isRunning() && contains(player.getUniqueId())) {

                if (isGracePeriod() && isRunner(player.getUniqueId())
                        || (isRunner(player.getUniqueId()) && isRunner(damager.getUniqueId()))
                        || (isHunter(player.getUniqueId()) && isHunter(damager.getUniqueId()))
                        || getInvincibility(damager.getUniqueId()) >= 0)
                    event.setCancelled(true);
                else
                    lastHits.put(player.getUniqueId(), getTime());

            }
        }
    }

    @Override
    public void onEntityDeath(EntityDeathEvent event) {

    }

    @Override
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (isRunning()) {

            DropHolder dropHolder = new DropHolder(event.getEntity(), event.getDrops(), event.getDroppedExp());
            drops.put(event.getEntity().getUniqueId(), dropHolder);

            Integer lastHit = lastHits.get(event.getEntity().getUniqueId());
            if (lastHit != null && getTime() - lastHit <= plugin.getConfig().getInt("game.combat-time") * 20) {

                if (isHunter(event.getEntity().getUniqueId()) && hunters.size() < 2) {
                    event.getDrops().clear();
                    event.setDroppedExp(0);
                }

            } else if (getRevives(event.getEntity().getUniqueId()) > 0) {

                event.getDrops().clear();
                event.setDroppedExp(0);
                canRevive.put(event.getEntity().getUniqueId(), true);
                return;

            }

            if (isRunner(event.getEntity().getUniqueId())) {
                broadcast(String.format(plugin.getConfigString("game.runner-killed"), event.getEntity().getName()));
                spectate(event.getEntity());

                runnersEliminated++;
                if (runnersEliminated >= runners.size())
                    win("hunters");
            }

        }
    }

    @Override
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (isRunning() && contains(event.getPlayer().getUniqueId())) {

            giveTrackingCompasses(event.getPlayer());

            if (!event.isBedSpawn() && !event.isAnchorSpawn()) {
                event.setRespawnLocation(getSpawnLocation(event.getPlayer().getUniqueId()));
            }

            Integer revives = getRevives(event.getPlayer().getUniqueId());
            if (revives != null && revives > 0 && canRevive.getOrDefault(event.getPlayer().getUniqueId(), false)) {
                canRevive.remove(event.getPlayer().getUniqueId());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (isHunter(event.getPlayer().getUniqueId())) {
                        plugin.getReviveGUI().open(event.getPlayer(), this);
                    } else {
                        revive(event.getPlayer());
                        event.getPlayer().sendMessage(String.format(plugin.getConfigString("revive-gui.remaining"), revives - 1));
                    }
                });
            }

        }
    }

    private void handlePortalTeleport(Entity entity, Location from, Location to, PlayerTeleportEvent.TeleportCause cause) {
        boolean inOverworld = from.getWorld().getEnvironment() == World.Environment.NORMAL;
        switch (cause) {

            case NETHER_PORTAL:
                to.setWorld(inOverworld ? nether : overworld);
                break;
            case END_PORTAL:
                if (inOverworld)
                    to.setWorld(end);
                else if (!(entity instanceof Player)) {
                    to.setWorld(overworld);
                    Location s = overworld.getSpawnLocation();
                    to.setX(s.getX());
                    to.setY(s.getY());
                    to.setZ(s.getZ());
                }
        }
    }

    @Override
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (isRunning() && event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            if (isRunner(event.getPlayer().getUniqueId())) {
                setNetherEntryTime();
            } else if (isHunter(event.getPlayer().getUniqueId()) && isNetherGracePeriod()) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(plugin.getConfigString("game.nether-entry-deny"));
            }
        }
        handlePortalTeleport(event.getPlayer(), event.getFrom(), event.getTo(), event.getCause());
    }

    @Override
    public void onEntityPortal(EntityPortalEvent event) {
        PlayerTeleportEvent.TeleportCause cause = event.getEntity().getLocation().getBlock().getType() == Material.END_PORTAL
                ? PlayerTeleportEvent.TeleportCause.END_PORTAL
                : PlayerTeleportEvent.TeleportCause.NETHER_PORTAL;
        handlePortalTeleport(event.getEntity(), event.getFrom(), event.getTo(), cause);
    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (isRunning() && !contains(event.getPlayer().getUniqueId()))
            spectate(event.getPlayer());
    }

    @Override
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {

    }

}
