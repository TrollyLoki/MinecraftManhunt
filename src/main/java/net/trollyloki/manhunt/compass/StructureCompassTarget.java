package net.trollyloki.manhunt.compass;

import net.trollyloki.manhunt.ManhuntPlugin;
import org.bukkit.Location;
import org.bukkit.generator.structure.Structure;
import org.bukkit.util.StructureSearchResult;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class StructureCompassTarget implements CompassTarget {

    protected final @NotNull Structure structure;

    protected StructureCompassTarget(@NotNull Structure structure) {
        this.structure = structure;
    }

    @Override
    public @NotNull Optional<Location> getLocation(@NotNull Location origin) {
        StructureSearchResult result = origin.getWorld().locateNearestStructure(origin, structure,
                ManhuntPlugin.getInstance().getConfig().getInt("tracking-compass.structure-search-radius"),
                false);
        return Optional.ofNullable(result).map(StructureSearchResult::getLocation);
    }

}
