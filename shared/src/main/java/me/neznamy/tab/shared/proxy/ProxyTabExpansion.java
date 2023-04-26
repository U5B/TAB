package me.neznamy.tab.shared.proxy;

import lombok.NonNull;
import me.neznamy.tab.shared.platform.TabPlayer;
import me.neznamy.tab.shared.placeholders.expansion.TabExpansion;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class ProxyTabExpansion implements TabExpansion {

    /** Map holding all values for all players for resending on server switch */
    private final Map<TabPlayer, Map<String, String>> values = new WeakHashMap<>();

    @Override
    public void setValue(@NonNull TabPlayer player, @NonNull String key, @NonNull String value) {
        values.computeIfAbsent(player, p -> new HashMap<>()).put(key, value);
        ((ProxyTabPlayer)player).sendPluginMessage("Expansion", key, value);
    }

    @Override
    public boolean unregister() {
        // Don't do anything on proxy side
        return false;
    }

    public void resendAllValues(TabPlayer player) {
        for (Map.Entry<String, String> entry : values.computeIfAbsent(player, p -> new HashMap<>()).entrySet()) {
            ((ProxyTabPlayer)player).sendPluginMessage("Expansion", entry.getKey(), entry.getValue());
        }
    }
}
