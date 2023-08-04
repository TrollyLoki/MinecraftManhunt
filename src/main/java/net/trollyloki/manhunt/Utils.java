package net.trollyloki.manhunt;

import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Utils {

    /**
     * Gets a random location in the given world
     *
     * @param world World
     * @param maxDistance Maximum distance from the center of the world
     */
    public static CompletableFuture<Location> getRandomSafeLocation(Plugin plugin, World world, int maxDistance) {
        final CompletableFuture<Location> future = new CompletableFuture<>();
        BukkitRunnable runnable = new BukkitRunnable() {

            @Override
            public void run() {
                Location location = tryRandomLocation(world, maxDistance);
                if (location != null) {
                    future.complete(location);
                    cancel();
                }
            }

        };
        runnable.runTaskTimer(plugin, 10, 10);
        return future;
    }

    public static Location tryRandomLocation(World world, int maxDistance) {
        int x = getRandomCoordinate(maxDistance);
        int z = getRandomCoordinate(maxDistance);
        Block block = world.getHighestBlockAt(x, z);
        if (!block.isLiquid())
            return block.getLocation().add(0.5, 1, 0.5);
        else
            return null;
    }

    /**
     * Gets a random coordinate
     *
     * @param maxDistance Maximum distance from 0
     */
    public static int getRandomCoordinate(int maxDistance) {
        return (int) ((Math.random() - 0.5) * 2 * maxDistance);
    }

    /**
     * Formats a time interval as a human-readable string
     *
     * @param ticks Number of ticks
     * @return Formatted string
     */
    public static String formatTime(int ticks) {

        String time = "";
        if (ticks < 0) { // handle negative time
            time += "-";
            ticks = -ticks;
        }

        int days = ticks / 1728000; ticks %= 1728000;
        int hours = ticks / 72000; ticks %= 72000;
        int minutes = ticks / 1200; ticks %= 1200;
        int seconds = ticks / 20; ticks %= 20;

        time += "%d:%02d";
        if (days > 0) {
            time += ":%02d:%02d";
            return String.format(time, days, hours, minutes, seconds);
        } else if (hours > 0) {
            time += ":%02d";
            return String.format(time, hours, minutes, seconds);
        } else {
            return String.format(time, minutes, seconds);
        }

    }

    public static final UUIDPersistentDataType UUID_PERSISTENT_DATA_TYPE = new UUIDPersistentDataType();

    private static class UUIDPersistentDataType implements PersistentDataType<byte[], UUID> {

        @Override
        public Class<byte[]> getPrimitiveType() {
            return byte[].class;
        }

        @Override
        public Class<UUID> getComplexType() {
            return UUID.class;
        }

        @Override
        public byte[] toPrimitive(UUID complex, PersistentDataAdapterContext context) {
            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.putLong(complex.getMostSignificantBits());
            buffer.putLong(complex.getLeastSignificantBits());
            return buffer.array();
        }

        @Override
        public UUID fromPrimitive(byte[] primitive, PersistentDataAdapterContext context) {
            ByteBuffer buffer = ByteBuffer.wrap(primitive);
            return new UUID(buffer.getLong(), buffer.getLong());
        }

    }

    /**
     * Deletes the given directory and all files in it
     *
     * @param directory Directory
     * @return {@code true} if the directory was deleted
     */
    public static boolean deleteDirectory(File directory) {
        Stack<File> stack = new Stack<>();
        stack.push(directory);
        while (!stack.isEmpty()) {
            File next = stack.pop();
            for (File file : next.listFiles()) {
                if (file.isDirectory()) {
                    stack.push(file);
                } else {
                    if (!file.delete())
                        return false;
                    file.getParentFile().delete(); // attempt to delete parent folder
                }
            }
        }
        return true;
    }

    /**
     * Revokes all advancements from a player.
     *
     * @param player Player
     * @param advancementIterator Advancement Iterator
     */
    public static void clearAdvancements(Player player, Iterator<Advancement> advancementIterator) {
        Iterator<Advancement> iter = player.getServer().advancementIterator();
        while (iter.hasNext()) {
            AdvancementProgress progress = player.getAdvancementProgress(iter.next());
            for (String criteria : progress.getAwardedCriteria())
                progress.revokeCriteria(criteria);
        }
    }

    /**
     * Converts a collection of players to a list of names
     *
     * @param players Players
     * @return List of names
     */
    public static List<String> getNames(Collection<? extends Player> players) {
        LinkedList<String> names = new LinkedList<>();
        for (Player player : players)
            names.add(player.getName());
        return names;
    }

    /**
     * Converts a collection of keyed objects to a list of string keys
     *
     * @param objects Objects
     * @return List of keys
     */
    public static List<String> getKeys(Collection<? extends Keyed> objects) {
        LinkedList<String> names = new LinkedList<>();
        for (Keyed keyed : objects)
            names.add(keyed.getKey().toString());
        return names;
    }

    /**
     * Filters a list
     *
     * @param list List
     * @param start Required prefix
     * @return The same list
     */
    public static List<String> filter(List<String> list, String start) {
        String finalStart = start.toLowerCase();
        list.removeIf(string -> !string.toLowerCase().startsWith(finalStart));
        return list;
    }

}
