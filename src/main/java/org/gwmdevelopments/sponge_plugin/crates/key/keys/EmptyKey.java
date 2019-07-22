package org.gwmdevelopments.sponge_plugin.crates.key.keys;

import ninja.leaping.configurate.ConfigurationNode;
import org.gwmdevelopments.sponge_plugin.crates.key.Key;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Optional;

public final class EmptyKey extends Key {

    public static final String TYPE = "EMPTY";

    public EmptyKey(ConfigurationNode node) {
        super(node);
    }

    public EmptyKey(Optional<String> id) {
        super(id, true);
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void withdraw(Player player, int amount, boolean force) {
    }

    @Override
    public int get(Player player) {
        return 1;
    }
}
