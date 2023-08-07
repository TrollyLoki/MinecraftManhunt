package net.trollyloki.manhunt.compass;

import net.trollyloki.manhunt.ManhuntPlugin;
import org.bukkit.generator.structure.Structure;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

public class FortressCompassTarget extends StructureCompassTarget {

    public FortressCompassTarget() {
        super(Structure.FORTRESS);
    }

    @Override
    public @NotNull String getName() {
        return ManhuntPlugin.getInstance().getConfig().getString("tracking-compass.fortress-name", "Fortress");
    }

    @Override
    public void write(@NotNull PersistentDataContainer container) {
        // nothing to write
    }

}
