package me.neznamy.tab.shared.features.layout;

import java.util.UUID;
import me.neznamy.tab.shared.TAB;
import me.neznamy.tab.shared.features.types.HideEntryListener;
import me.neznamy.tab.shared.features.types.JoinListener;
import me.neznamy.tab.shared.features.types.Loadable;
import me.neznamy.tab.shared.features.types.TabFeature;
import me.neznamy.tab.shared.features.types.UnLoadable;
import me.neznamy.tab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Layout sub-feature updating latency of layout entries to match player latencies.
 */
public class LayoutHideRealPlayers extends TabFeature implements JoinListener, Loadable, UnLoadable, HideEntryListener {

    /**
     * Constructs new instance.
     */
    public LayoutHideRealPlayers() {
        super();
    }

    @Override
    public String getFeatureName() {
        return "Layout (Hide Players)";
    }

    @Override
    public boolean shouldHideEntry(TabPlayer viewer, UUID packetId, boolean show) {
        if (TAB.getInstance().getPlayerByTabListUUID(packetId) != null) {
            return false;
        }
        return show;
    }

    @Override
    public void load() {
        updateAll(false);
    }

    @Override
    public void unload() {
        updateAll(true);
    }

    @Override
    public void onJoin(@NotNull TabPlayer connectedPlayer) {
        for (TabPlayer all : TAB.getInstance().getOnlinePlayers()) {
            connectedPlayer.getTabList().updateListed(all.getTablistId(), false);
            all.getTabList().updateListed(connectedPlayer.getTablistId(), false);
        }
    }

    private void updateAll(boolean show) {
        for (TabPlayer viewer : TAB.getInstance().getOnlinePlayers()) {
            for (TabPlayer target : TAB.getInstance().getOnlinePlayers()) {
                viewer.getTabList().updateListed(target.getTablistId(), show);
            }
        }
    }
}
