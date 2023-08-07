package net.trollyloki.manhunt.compass;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class PlayerCompassTarget implements CompassTarget {

    private static final @NotNull NamespacedKey UUID_KEY = new NamespacedKey("speedrun", "uuid");

    private final @NotNull UUID uuid;

    public PlayerCompassTarget(@NotNull Player player) {
        this.uuid = player.getUniqueId();
    }

    public PlayerCompassTarget(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    protected PlayerCompassTarget(@NotNull PersistentDataContainer container) {
        long[] uuidLongs = container.get(UUID_KEY, PersistentDataType.LONG_ARRAY);
        if (uuidLongs == null || uuidLongs.length < 2)
            throw new IllegalArgumentException("Invalid UUID long data");
        this.uuid = new UUID(uuidLongs[0], uuidLongs[1]);
    }

    @Override
    public void write(@NotNull PersistentDataContainer container) {
        long[] uuidLongs = {uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()};
        container.set(UUID_KEY, PersistentDataType.LONG_ARRAY, uuidLongs);
    }

    @Override
    public @NotNull Optional<Location> getLocation(@NotNull Location origin) {
        return Optional.ofNullable(Bukkit.getPlayer(uuid)).map(Player::getLocation);
    }

    @Override
    public @NotNull String getName() {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        if (name != null)
            return name;
        return uuid.toString();
    }

}
