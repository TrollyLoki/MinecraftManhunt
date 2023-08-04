package net.trollyloki.manhunt;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ReviveGUI implements Listener {

    private final ManhuntPlugin plugin;
    private final Map<InventoryView, AbstractManhunt> inventories;

    public ReviveGUI(ManhuntPlugin plugin) {
        this.plugin = plugin;
        this.inventories = new HashMap<>();
    }

    public void open(Player player, AbstractManhunt manhunt) {
        Inventory inventory = player.getServer().createInventory(null, 27, plugin.getConfigString("revive-gui.title"));

        Integer revives = manhunt.getRevives(player.getUniqueId());
        String lore = String.format(plugin.getConfigString("revive-gui.remaining"), revives);
        ItemStack yes = getItemStack(plugin.getConfig().getConfigurationSection("revive-gui.yes-item"), lore);
        ItemStack no = getItemStack(plugin.getConfig().getConfigurationSection("revive-gui.no-item"), null);
        inventory.setItem(11, yes);
        inventory.setItem(15, no);
        inventories.put(player.openInventory(inventory), manhunt);
    }

    public static ItemStack getItemStack(ConfigurationSection config, String lore) {
        ItemStack item = new ItemStack(Material.valueOf(config.getString("material")));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("name")));
        if (lore != null)
            meta.setLore(Collections.singletonList(lore));
        item.setItemMeta(meta);
        return item;
    }

    public void close(Player player) {
        inventories.remove(player.getOpenInventory());
        player.closeInventory();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        AbstractManhunt manhunt = inventories.get(event.getView());
        if (manhunt != null) {

            event.setCancelled(true);
            if (event.getSlot() == 11) {
                close((Player) event.getWhoClicked());
                manhunt.revive((Player) event.getWhoClicked());
            } else if (event.getSlot() == 15) {
                close((Player) event.getWhoClicked());
                manhunt.dropItems((Player) event.getWhoClicked());
            }

        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        AbstractManhunt manhunt = inventories.get(event.getView());
        if (manhunt != null) {
            close((Player) event.getPlayer());
            manhunt.dropItems((Player) event.getPlayer());
        }
    }

}
