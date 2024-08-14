package me.neznamy.tab.shared.features.layout;

import java.util.UUID;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.neznamy.tab.shared.TAB;
import me.neznamy.tab.shared.TabConstants;
import me.neznamy.tab.shared.config.files.config.LayoutConfiguration.LayoutDefinition.FixedSlotDefinition;
import me.neznamy.tab.shared.features.types.RefreshableFeature;
import me.neznamy.tab.shared.platform.TabList;
import me.neznamy.tab.shared.platform.TabPlayer;
import me.neznamy.tab.shared.util.cache.StringToComponentCache;
import org.jetbrains.annotations.NotNull;

/**
 * A fixed layout slot with defined slot, text and maybe also ping and skin.
 */
@RequiredArgsConstructor
public class FixedSlot extends RefreshableFeature {

    private static final StringToComponentCache cache = new StringToComponentCache("LayoutFixedSlot", 1000);

    @NonNull private final LayoutManagerImpl manager;
    @Getter private final int slot;
    @NonNull private final LayoutPattern pattern;
    @Getter @NonNull private final UUID id;
    @NonNull private final String text;
    @Getter @NonNull private final String propertyName;
    @NonNull private final String skin;
    @Getter @NonNull private final String skinProperty;
    private final int ping;

    @NotNull
    @Override
    public String getFeatureName() {
        return manager.getFeatureName();
    }

    @NotNull
    @Override
    public String getRefreshDisplayName() {
        return "Updating fixed slots";
    }

    @Override
    public void refresh(@NotNull TabPlayer p, boolean force) {
        if (p.layoutData.view == null || p.layoutData.view.getPattern() != pattern ||
                p.getVersion().getMinorVersion() < 8 || p.isBedrockPlayer()) return; // TODO check if / make view null for <1.8 and bedrock to skip all these checks everywhere
        if (p.getProperty(skinProperty).update()) {
            p.getTabList().removeEntry(id);
            p.getTabList().addEntry(createEntry(p));
        } else {
            if (p.getProperty(propertyName).update()) {
                p.getTabList().updateDisplayName(id, cache.get(p.getProperty(propertyName).get()));
            }
        }
    }

    /**
     * Creates a tablist entry from this slot for given viewer.
     *
     * @param   viewer
     *          Player viewing the slot
     * @return  Tablist entry from this slot
     */
    public @NotNull TabList.Entry createEntry(@NotNull TabPlayer viewer) {
        viewer.setProperty(this, propertyName, text);
        viewer.setProperty(this, skinProperty, skin);
        return new TabList.Entry(
                id,
                manager.getConfiguration().direction.getEntryName(viewer, slot, LayoutManagerImpl.isTeamsEnabled()),
                manager.getSkinManager().getSkin(viewer.getProperty(skinProperty).updateAndGet()),
                true,
                ping,
                0,
                cache.get(viewer.getProperty(propertyName).updateAndGet())
        );
    }

    /**
     * Update an existing entry from this slot for given viewer. This doesn't work for skins!
     *
     * @param   viewer
     *          Player viewing the slot
     * @return  returns false if update unsuccessful, otherwise returns true
     */
    public boolean updateEntry(@NotNull TabPlayer viewer, LayoutView previousLayout) {
        if (previousLayout == null || !viewer.getTabList().containsEntry(id)) {
            return false;
        }
        FixedSlot previousSlot = previousLayout.getFixedSlots().stream().filter(x -> x.getId() == id).findAny().orElse(null);
        // Fail if previousSlot skin doesn't equal new skin
        if (previousSlot == null || !previousSlot.skin.equals(skin)) {
            return false;
        }
        viewer.setProperty(this, propertyName, text);
        viewer.setProperty(this, skinProperty, skin);
        if (viewer.getProperty(propertyName).update()) {
            viewer.getTabList().updateDisplayName(id, cache.get(viewer.getProperty(propertyName).get()));
        }
        if (previousSlot.ping != ping) {
            viewer.getTabList().updateLatency(id, ping);
        }
        return true;
    }

    /**
     * Creates a new instance with given parameters.
     *
     * @param   def
     *          Fixed slot definition
     * @param   pattern
     *          Layout this slot belongs to
     * @param   manager
     *          Layout manager
     * @return  New slot using given line or {@code null} if invalid
     */
    @NotNull
    public static FixedSlot fromDefinition(@NotNull FixedSlotDefinition def, @NotNull LayoutPattern pattern, @NotNull LayoutManagerImpl manager) {
        FixedSlot f = new FixedSlot(
                manager,
                def.slot,
                pattern,
                manager.getUUID(def.slot),
                def.text,
                "Layout-" + pattern.getName() + "-SLOT-" + def.slot,
                def.skin == null || def.skin.isEmpty() ? manager.getConfiguration().getDefaultSkin(def.slot) : def.skin,
                "Layout-" + pattern.getName() + "-SLOT-" + def.slot + "-skin",
                def.ping == null ? manager.getConfiguration().emptySlotPing : def.ping
        );
        if (!def.text.isEmpty()) TAB.getInstance().getFeatureManager().registerFeature(TabConstants.Feature.layoutSlot(pattern.getName(), def.slot), f);
        return f;
    }
}