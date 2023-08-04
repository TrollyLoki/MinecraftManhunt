package net.trollyloki.manhunt;

import net.trollyloki.manhunt.types.AdvancementManhunt;
import net.trollyloki.manhunt.types.ClassicManhunt;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ManhuntCommand implements CommandExecutor, TabCompleter {

    private static final String RELOAD_PERM = "manhunt.reload", ADMIN_PERM = "manhunt.admin";

    private final ManhuntPlugin plugin;
    private final HashMap<UUID, AbstractManhunt> manhunts = new HashMap<>();
    private final HashMap<CommandSender, BukkitTask> resetConfirm = new HashMap<>();

    public ManhuntCommand(ManhuntPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * This method takes a player and returns the manhunt they are currently owner of.
     * @param sender Sender of command
     * @return Manhunt that they own, null if none
     */
    public AbstractManhunt getManhuntAsOwner(CommandSender sender) {
        AbstractManhunt manhunt = null;
        if (sender instanceof Player)
            manhunt = manhunts.get(((Player) sender).getUniqueId());
        if (manhunt == null) {
            // Check if user is a participant in a manhunt
            if (getManhuntAsMember(sender) != null)
                sender.sendMessage(ChatColor.RED + "You are not the owner of this manhunt");
            else
                sender.sendMessage(ChatColor.RED + "You do not own a manhunt");
        }
        return manhunt;
    }

    /**
     * This method takes a player and returns any manhunt they are participating in OR owner of.
     * Being the owner does not always mean they are participating. The owner needs to add themselves to the manhunt.
     * However, that should not matter with this method.
     * @param sender Sender of command
     * @return Manhunt that they are participating in OR owner of, null if none
     */
    public AbstractManhunt getManhunt(CommandSender sender) {
        AbstractManhunt manhunt = null;
        // Check if player is a participant in a manhunt
        // Owning a manhunt does not mean you are participating in the manhunt (yet)
        if (sender instanceof Player player) {
            for (AbstractManhunt tempHunt : manhunts.values()) {
                if (tempHunt.getPlayers().contains(player)) {
                    manhunt = tempHunt;
                    break;
                }
            }
            // If they were not found, maybe they own a manhunt but just haven't added themselves yet
            if (manhunt == null)
                manhunt = manhunts.get(player.getUniqueId());
        }

        if (manhunt == null)
            sender.sendMessage(ChatColor.RED + "You are not in a manhunt");
        return manhunt;
    }

    /**
     * This method takes a player and returns any manhunt they are participating in OR owner of.
     * However, this does not send any messages to the sender.
     * Being the owner does not always mean they are participating. The owner needs to add themselves to the manhunt.
     * However, that should not matter with this method.
     * @param sender Sender of command
     * @return Manhunt that they are participating in OR owner of, null if none
     */
    public AbstractManhunt getManhuntSilently(CommandSender sender) {
        AbstractManhunt manhunt = null;
        // Check if player is a participant in a manhunt
        // Owning a manhunt does not mean you are participating in the manhunt (yet)
        if (sender instanceof Player player) {
            for (AbstractManhunt tempHunt : manhunts.values()) {
                if (tempHunt.getPlayers().contains(player)) {
                    manhunt = tempHunt;
                    break;
                }
            }
            // If they were not found, maybe they own a manhunt but just haven't added themselves yet
            if (manhunt == null)
                manhunt = manhunts.get(player.getUniqueId());
        }

        return manhunt;
    }

    /**
     * This method takes a player and returns any manhunt they are participating in.
     * Being the owner does not always mean they are participating. The owner needs to add themselves to the manhunt.
     * @param sender Sender of command
     * @return Manhunt associated with the sender, null if none
     */
    public AbstractManhunt getManhuntAsMember(CommandSender sender) {
        AbstractManhunt manhunt = null;
        // Check if player is a participant in a manhunt
        // Owning a manhunt does not mean you are participating in the manhunt (yet)
        if (sender instanceof Player player) {
            for (AbstractManhunt tempHunt : manhunts.values()) {
                if (tempHunt.getPlayers().contains(player)) {
                    manhunt = tempHunt;
                    break;
                }
            }
        }

        return manhunt;
    }



    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if (args.length > 0) {

            // Compass command - usable by anyone currently in a manhunt
            // Only hunters and players with admin perms may obtain compasses
            if (args[0].equalsIgnoreCase("compass")) {

                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can get tracking compasses");
                    return false;
                }

                AbstractManhunt manhunt = getManhunt(sender);
                if (manhunt != null) {

                    if (!sender.hasPermission(ADMIN_PERM) && !manhunt.isHunter(player.getUniqueId())) {
                        sender.sendMessage(ChatColor.RED + "Only hunters can get tracking compasses");
                        return false;
                    }

                    if (args.length > 1) {

                        Player target = plugin.getServer().getPlayerExact(args[1]);
                        // If you have admin perms you can get compasses for hunters.
                        // If not, you can only get them for runners.
                        if (target == null || (!sender.hasPermission(ADMIN_PERM) && !manhunt.isRunner(target.getUniqueId()))) {
                            sender.sendMessage(ChatColor.RED + "You can only get tracking compasses for runners");
                            return false;
                        }

                        ItemStack compass = plugin.getListener().getTrackingCompass(target.getUniqueId());
                        if (compass == null) {
                            sender.sendMessage(ChatColor.RED + "Failed to get a tracking compass for " + target.getName());
                            return false;
                        }

                        if (!player.getInventory().addItem(compass).isEmpty())
                            player.getWorld().dropItem(player.getLocation(), compass);
                        sender.sendMessage(ChatColor.GREEN + "You have been given a tracking compass for " + target.getName());
                        return true;

                    }

                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " compass <player>");
                }
                return false;

            }

            // Reload command - usable by anyone with admin perms
            else if (args[0].equalsIgnoreCase("reload")) {

                if (!sender.hasPermission(RELOAD_PERM)) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to reload the config");
                    return false;
                }

                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "The config has been reloaded");
                return true;

            }

            // Create command - usable by anyone with admin perms
            else if (args[0].equalsIgnoreCase("create")) {

                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can create manhunts");
                    return false;
                }

                if (!sender.hasPermission(ADMIN_PERM)) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to create a manhunt");
                    return false;
                }

                if (args.length > 1) {

                    if (args[1].equalsIgnoreCase("abstract")) {

                        AbstractManhunt manhunt = new AbstractManhunt(plugin);
                        manhunts.put(player.getUniqueId(), manhunt);
                        sender.sendMessage(ChatColor.GREEN + "Created a new abstract manhunt");
                        return true;

                    } else if (args[1].equalsIgnoreCase("classic")) {

                        ClassicManhunt manhunt = new ClassicManhunt(plugin);
                        manhunts.put(player.getUniqueId(), manhunt);
                        sender.sendMessage(ChatColor.GREEN + "Created a new classic manhunt");
                        return true;

                    } else if (args[1].equalsIgnoreCase("advancement")) {

                        Advancement goal;
                        if (args.length > 2) {
                            NamespacedKey key = NamespacedKey.fromString(args[2]);
                            if (key == null || (goal = plugin.getServer().getAdvancement(key)) == null) {
                                sender.sendMessage(ChatColor.RED + "Invalid advancement key");
                                return false;
                            }
                        } else {
                            List<Advancement> list = getAdvancementList();
                            goal = list.get((int) (Math.random() * list.size()));
                        }
                        AdvancementManhunt manhunt = new AdvancementManhunt(plugin, goal);
                        manhunts.put(player.getUniqueId(), manhunt);
                        sender.sendMessage(ChatColor.GREEN + "Created a new advancement manhunt (" + goal.getKey() + ")");
                        return true;

                    }

                }

                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " create <abstract|classic|advancement> [goal]");
                return false;

            }

            // Add command - usable by anyone with admin perms currently in a manhunt, owner or not
            else if (args[0].equalsIgnoreCase("add")) {

                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can add players");
                    return false;
                }

                AbstractManhunt manhunt = getManhunt(sender);
                if (manhunt != null) {

                    if (!sender.hasPermission(ADMIN_PERM)) {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to add players");
                        return false;
                    }

                    if (args.length > 2) {

                        Player toAdd = plugin.getServer().getPlayerExact(args[2]);
                        if (toAdd == null) {
                            sender.sendMessage(ChatColor.RED + args[2] + " is not online");
                            return false;
                        }

                        if (args[1].equalsIgnoreCase("runner")) {

                            if (manhunt.addRunner(toAdd.getUniqueId())) {
                                sender.sendMessage(ChatColor.GREEN + toAdd.getName() + " is now a runner");
                                return true;
                            } else {
                                sender.sendMessage(ChatColor.RED + toAdd.getName() + " is already a runner");
                                return false;
                            }

                        } else if (args[1].equalsIgnoreCase("hunter")) {

                            if (manhunt.addHunter(toAdd.getUniqueId())) {
                                sender.sendMessage(ChatColor.GREEN + toAdd.getName() + " is now a hunter");
                                return true;
                            } else {
                                sender.sendMessage(ChatColor.RED + toAdd.getName() + " is already a hunter");
                                return false;
                            }

                        }

                    }

                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " add <runner|hunter> <player>");
                }
                return false;

            }

            // Remove command - usable by anyone with admin perms currently in a manhunt, owner or not
            else if (args[0].equalsIgnoreCase("remove")) {

                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can remove players");
                    return false;
                }

                AbstractManhunt manhunt = getManhunt(sender);
                if (manhunt != null) {

                    if (!sender.hasPermission(ADMIN_PERM)) {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to remove players");
                        return false;
                    }

                    if (args.length > 1) {

                        Player toRemove = plugin.getServer().getPlayerExact(args[1]);
                        if (toRemove == null) {
                            sender.sendMessage(ChatColor.RED + args[1] + " is not online");
                            return false;
                        }

                        if (manhunt.remove(toRemove.getUniqueId())) {
                            sender.sendMessage(ChatColor.GREEN + toRemove.getName() + " was removed from the manhunt");
                            return true;
                        } else {
                            sender.sendMessage(ChatColor.RED + toRemove.getName() + " is not in the manhunt");
                            return false;
                        }

                    }

                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " remove <player>");
                }
                return false;

            }

            // Start command - only the owner may start manhunts
            else if (args[0].equalsIgnoreCase("start")) {

                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can start manhunts");
                    return false;
                }

                AbstractManhunt manhunt = getManhuntAsOwner(sender);
                if (manhunt != null) {

                    if (!sender.hasPermission(ADMIN_PERM)) {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to start the manhunt");
                        return false;
                    }

                    try {
                        if (!manhunt.isRunning()) {
                            sender.sendMessage(ChatColor.YELLOW + "Creating worlds...");
                            plugin.getListener().registerManhunt(manhunt).thenAccept((b) -> {
                                sender.sendMessage(ChatColor.YELLOW + "World creation complete! Starting manhunt...");
                                manhunt.start(msg -> sender.sendMessage(ChatColor.YELLOW + msg));
                                sender.sendMessage(ChatColor.GREEN + "Started the manhunt");
                            });
                            return true;
                        } else {
                            sender.sendMessage(ChatColor.RED + "The manhunt is already running");
                            return false;
                        }
                    } catch (IllegalStateException e) {
                        sender.sendMessage(ChatColor.RED + "Failed to start the manhunt: " + e.getMessage());
                        return false;
                    }

                }

            }

            // Revive command - usable by anyone with admin perms currently in a manhunt, owner or not
            else if (args[0].equalsIgnoreCase("revive")) {

                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can revive players");
                    return false;
                }

                AbstractManhunt manhunt = getManhunt(sender);
                if (manhunt != null) {

                    if (!sender.hasPermission(ADMIN_PERM)) {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to revive players");
                        return false;
                    }

                    if (args.length > 1) {

                        Player toRevive = plugin.getServer().getPlayerExact(args[1]);
                        if (toRevive == null) {
                            sender.sendMessage(ChatColor.RED + args[1] + " is not online");
                            return false;
                        }

                        Integer remainingRevives = manhunt.revive(toRevive);
                        if (remainingRevives != null) {
                            sender.sendMessage(ChatColor.GREEN + toRevive.getName() + " was revived (" + remainingRevives + " revives remaining)");
                            return true;
                        } else {
                            sender.sendMessage(ChatColor.RED + "Failed to revive " + toRevive.getName());
                            return false;
                        }

                    }

                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " revive <player>");
                }
                return false;

            }

            // Pause command - usable by anyone with admin perms currently in a manhunt, owner or not
            else if (args[0].equalsIgnoreCase("pause")) {

                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can pause manhunts");
                    return false;
                }

                AbstractManhunt manhunt = getManhunt(sender);
                if (manhunt != null) {

                    if (!sender.hasPermission(ADMIN_PERM)) {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to pause the manhunt");
                        return false;
                    }

                    if (!manhunt.isPaused()) {
                        manhunt.setPaused(true);
                        sender.sendMessage(ChatColor.GREEN + "Paused the manhunt");
                    } else {
                        manhunt.setPaused(false);
                        sender.sendMessage(ChatColor.GREEN + "Resumed the manhunt");
                    }

                    return true;
                }

            }

            // Stop command - only the owner may stop manhunts
            else if (args[0].equalsIgnoreCase("stop")) {

                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can stop manhunts");
                    return false;
                }

                AbstractManhunt manhunt = getManhuntAsOwner(sender);
                if (manhunt != null) {

                    if (!sender.hasPermission(ADMIN_PERM)) {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to stop the manhunt");
                        return false;
                    }

                    if (manhunt.stop()) {
                        plugin.getListener().unregisterManhunt(manhunt);
                        manhunts.values().removeIf(m -> m == manhunt);
                        sender.sendMessage(ChatColor.GREEN + "Stopped the manhunt");
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "The manhunt is not running");
                        return false;
                    }

                }

            }

            // Reset command - usable by anyone with admin perms
            // COMPLETELY USELESS
            else if (args[0].equalsIgnoreCase("reset")) {

                if (!sender.hasPermission(ADMIN_PERM)) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to reset the world");
                    return false;
                }

                if (resetConfirm.containsKey(sender)) {
                    resetConfirm.remove(sender).cancel();
                    plugin.resetWorld();
                    sender.sendMessage(ChatColor.GREEN + "Resetting the world...");
                } else {
                    BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin,
                            () -> resetConfirm.remove(sender), 600);
                    resetConfirm.put(sender, task);
                    sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "WARNING!"
                            + ChatColor.YELLOW + " This will delete the current world and "
                            + ChatColor.RED + ChatColor.BOLD + "CANNOT BE UNDONE!"
                            + ChatColor.YELLOW + " Type " + ChatColor.GOLD + "/" + label + " reset"
                            + ChatColor.YELLOW + " again within 30 seconds to confirm you want to reset the world.");
                }

                return true;

            }

        }

        String help = "compass";
        if (sender.hasPermission(RELOAD_PERM))
            help += "|reload";
        if (sender.hasPermission(ADMIN_PERM))
            help += "|create|add|remove|start|revive|pause|stop|reset";
        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <" + help + ">");
        return false;

    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if (args.length > 1) {
            AbstractManhunt manhunt;

            if (args[0].equalsIgnoreCase("compass")) {

                if (sender instanceof Player player) {

                    manhunt = getManhuntSilently(player);

                    if (args.length == 2 && manhunt != null) {
                        return Utils.filter(Utils.getNames(manhunt.getRunners()), args[1]);
                    }

                }

            }

            if (args[0].equalsIgnoreCase("create")) {

                if (sender.hasPermission(ADMIN_PERM)) {

                    if (args.length == 2) {
                        LinkedList<String> types = new LinkedList<>();
                        types.add("abstract");
                        types.add("classic");
                        types.add("advancement");
                        return Utils.filter(types, args[1]);
                    } else if (args.length == 3 && args[1].equalsIgnoreCase("advancement")) {
                        return Utils.filter(Utils.getKeys(getAdvancementList()), args[2]);
                    }

                }

            }

            else if (args[0].equalsIgnoreCase("add")) {

                if (sender.hasPermission(ADMIN_PERM)) {

                    if (args.length == 2) {
                        LinkedList<String> teams = new LinkedList<>();
                        teams.add("runner");
                        teams.add("hunter");
                        return Utils.filter(teams, args[1]);
                    } else if (args.length == 3) {
                        return Utils.filter(Utils.getNames(plugin.getServer().getOnlinePlayers()), args[2]);
                    }

                }

            }

            else if (args[0].equalsIgnoreCase("remove")
                    || args[0].equalsIgnoreCase("revive")
                    || args[0].equalsIgnoreCase("compass")) {

                if (sender.hasPermission(ADMIN_PERM) && sender instanceof Player
                        && (manhunt = manhunts.get(((Player) sender).getUniqueId())) != null) {

                    if (args.length == 2) {
                        return Utils.filter(Utils.getNames(manhunt.getPlayers()), args[1]);
                    }

                }

            }

        }

        else { // args.length <= 1

            LinkedList<String> subcommands = new LinkedList<>();
            subcommands.add("compass");
            if (sender.hasPermission(RELOAD_PERM))
                subcommands.add("reload");
            if (sender.hasPermission(ADMIN_PERM)) {
                subcommands.add("create");
                subcommands.add("add");
                subcommands.add("remove");
                subcommands.add("start");
                subcommands.add("revive");
                subcommands.add("pause");
                subcommands.add("stop");
                subcommands.add("reset");
            }

            if (args.length > 0)
                Utils.filter(subcommands, args[0]);
            return subcommands;

        }

        return new LinkedList<>();

    }

    public List<Advancement> getAdvancementList() {
        ArrayList<Advancement> list = new ArrayList<>();
        Iterator<Advancement> iter = plugin.getServer().advancementIterator();
        while (iter.hasNext()) {
            Advancement advancement = iter.next();
            if (!advancement.getKey().getKey().startsWith("recipes"))
                list.add(advancement);
        }
        return list;
    }

}
