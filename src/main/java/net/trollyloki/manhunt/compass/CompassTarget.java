package net.trollyloki.manhunt.compass;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface CompassTarget {

    @NotNull NamespacedKey TYPE_KEY = new NamespacedKey("speedrun", "compass_type");

    /**
     * Gets the current location of the target.
     *
     * @param origin current location of the compass
     * @return optional location
     */
    @NotNull Optional<Location> getLocation(@NotNull Location origin);

    /**
     * Gets the name of the target.
     *
     * @return name
     */
    @NotNull String getName();

    /**
     * Saves the target to a persistent data container.
     *
     * @param container data container
     */
    void write(@NotNull PersistentDataContainer container);

    /**
     * Saves this compass target to a persistent data container.
     *
     * @param container data container
     */
    default void save(@NotNull PersistentDataContainer container) {
        String type;
        if (getClass() == PlayerCompassTarget.class)
            type = "PLAYER";
        else if (getClass() == FortressCompassTarget.class)
            type = "FORTRESS";
        else if (getClass() == BastionCompassTarget.class)
            type = "BASTION";
        else
            return;

        container.set(TYPE_KEY, PersistentDataType.STRING, type);
        write(container);
    }

    /**
     * Loads a compass target from a persistent data container.
     *
     * @param container data container.
     * @return optional compass target
     */
    static @NotNull Optional<CompassTarget> load(@NotNull PersistentDataContainer container) {
        String type = container.get(TYPE_KEY, PersistentDataType.STRING);
        if (type == null)
            return Optional.empty();
        return switch (type) {
            case "PLAYER" -> Optional.of(new PlayerCompassTarget(container));
            case "FORTRESS" -> Optional.of(new FortressCompassTarget());
            case "BASTION" -> Optional.of(new BastionCompassTarget());
            default -> Optional.empty();
        };
    }

}
