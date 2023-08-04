package net.trollyloki.manhunt;

import org.bukkit.Location;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedList;
import java.util.List;

public class DropHolder {

    private final Location location;
    private final ItemStack[] items;
    private final int exp;
    private final List<ItemStack> droppedItems;
    private final int droppedExp;

    /**
     * Constructs a new drop holder with the given items and experience
     *
     * @param player Player
     * @param droppedItems Dropped items
     * @param droppedExp Dropped experience
     */
    public DropHolder(Player player, List<ItemStack> droppedItems, int droppedExp) {
        this.location = player.getLocation();
        this.items = player.getInventory().getContents();
        this.exp = player.getTotalExperience();
        this.droppedItems = new LinkedList<>(droppedItems);
        this.droppedExp = droppedExp;
    }

    /**
     * Gives the given player the contents of this drop holder
     *
     * @param player Player
     */
    public void give(Player player) {
        player.getInventory().setContents(items);
        player.setTotalExperience(exp);
        player.teleport(location);
    }

    /**
     * Drops the contents of this drop holder
     */
    public void drop() {
        if (location.getWorld() == null)
            return;
        for (ItemStack item : droppedItems) {
            if (item != null)
                location.getWorld().dropItemNaturally(location, item);
        }
        location.getWorld().spawn(location, ExperienceOrb.class).setExperience(droppedExp);
    }

}
