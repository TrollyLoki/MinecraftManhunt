package net.trollyloki.manhunt.compass;

import net.kyori.adventure.text.Component;
import net.trollyloki.manhunt.ManhuntPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CompassListener implements Listener {

    private final @NotNull ManhuntPlugin plugin;

    public CompassListener(@NotNull ManhuntPlugin plugin) {
        this.plugin = plugin;
    }

    public static void updateCompass(@NotNull ItemStack itemStack, @NotNull Location origin) {
        itemStack.editMeta(CompassMeta.class, compass -> {
            CompassTarget.load(compass.getPersistentDataContainer()).ifPresent(target -> {
                compass.setDisplayName(ManhuntPlugin.getInstance().getConfigString("tracking-compass.name-prefix") + target.getName());
                compass.setLore(List.of(ManhuntPlugin.getInstance().getConfigString("tracking-compass.lore")));

                target.getLocation(origin).ifPresent(location -> {
                    compass.setLodestoneTracked(false);
                    compass.setLodestone(location);
                });
            });
        });
    }

    public static @NotNull ItemStack createCompassItem(@NotNull CompassTarget target) {
        ItemStack itemStack = new ItemStack(Material.COMPASS);
        itemStack.editMeta(meta -> target.save(meta.getPersistentDataContainer()));
        return itemStack;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        if (!event.getAction().isRightClick())
            return;

        ItemStack item = event.getItem();
        if (item == null)
            return;

        updateCompass(item, event.getPlayer().getLocation());
    }

}
