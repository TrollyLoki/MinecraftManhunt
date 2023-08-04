package net.trollyloki.manhunt.types;

import net.trollyloki.manhunt.AbstractManhunt;
import net.trollyloki.manhunt.ManhuntPlugin;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDeathEvent;

public class ClassicManhunt extends AbstractManhunt {

    /**
     * Constructs a new classic manhunt with the runners' win condition being the Ender Dragon dying.
     *
     * @param plugin Plugin
     */
    public ClassicManhunt(ManhuntPlugin plugin) {
        super(plugin);
    }

    @Override
    protected int getStartingPearlCount() {
        return getPlugin().getConfig().getInt("game.pearls");
    }

    @Override
    public void onEntityDeath(EntityDeathEvent event) {
        super.onEntityDeath(event);
        if (event.getEntityType() == EntityType.ENDER_DRAGON)
            win("runners");
    }

}
