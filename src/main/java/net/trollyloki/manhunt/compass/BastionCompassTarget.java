package net.trollyloki.manhunt.compass;

import net.trollyloki.manhunt.ManhuntPlugin;
import org.bukkit.generator.structure.Structure;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

public class BastionCompassTarget extends StructureCompassTarget {

    public BastionCompassTarget() {
        super(Structure.BASTION_REMNANT);
    }

    @Override
    public @NotNull String getName() {
        return ManhuntPlugin.getInstance().getConfig().getString("tracking-compass.bastion-name", "Bastion");
    }

    @Override
    public void write(@NotNull PersistentDataContainer container) {
        // nothing to write
    }

}
