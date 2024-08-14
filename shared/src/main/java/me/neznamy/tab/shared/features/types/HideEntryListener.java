package me.neznamy.tab.shared.features.types;
import me.neznamy.tab.shared.platform.TabPlayer;

import java.util.UUID;

/**
 * An interface for features listening to ping change packet
 */
public interface HideEntryListener {

    /**
     * Called whenever player is being hidden or shown on tablist
     * @param   viewer
     *          Player who received the packet
     * @param   packetId
     *          UUID of the entry
     * @param   show
     *          Show entry on tablist
     * @return  true to show entry, false to hide entry
     */
    boolean shouldHideEntry(TabPlayer viewer, UUID packetId, boolean show);
}
