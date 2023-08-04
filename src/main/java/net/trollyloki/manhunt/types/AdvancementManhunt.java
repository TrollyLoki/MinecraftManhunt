package net.trollyloki.manhunt.types;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.trollyloki.manhunt.AbstractManhunt;
import net.trollyloki.manhunt.ManhuntPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class AdvancementManhunt extends AbstractManhunt {

    private final NamespacedKey goal;

    /**
     * Constructs a new advancement manhunt with the runners' win condition being obtaining the given advancement.
     *
     * @param plugin Plugin
     * @param advancement Goal advancement
     */
    public AdvancementManhunt(ManhuntPlugin plugin, Advancement advancement) {
        super(plugin);
        this.goal = advancement.getKey();
    }

    @Override
    public boolean start() {
        if (super.start()) {

            String key = "advancements." + goal.getKey().replaceAll("/", ".");
            TranslatableComponent component = new TranslatableComponent(key + ".title");
            component.setHoverEvent(
                    new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new Text(
                                    new BaseComponent[]{ new TranslatableComponent(key + ".description") }
                            )
                    )
            );

            String message = getPlugin().getConfigString("game.advancement-goal");
            BaseComponent[] components = { new TextComponent(message), component };
            for (Player player : getPlugin().getServer().getOnlinePlayers())
                player.spigot().sendMessage(components);

            return true;
        } else
            return false;
    }

    @Override
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        super.onPlayerAdvancementDone(event);
        if (event.getAdvancement().getKey().equals(goal) && isRunner(event.getPlayer().getUniqueId()))
            win("runners");
    }
}
