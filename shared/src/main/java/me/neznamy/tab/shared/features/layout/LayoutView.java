package me.neznamy.tab.shared.features.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Getter;
import me.neznamy.tab.shared.TAB;
import me.neznamy.tab.shared.chat.SimpleComponent;
import me.neznamy.tab.shared.config.files.config.LayoutConfiguration.LayoutDefinition.GroupPattern;
import me.neznamy.tab.shared.placeholders.conditions.Condition;
import me.neznamy.tab.shared.platform.TabList;
import me.neznamy.tab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class LayoutView {

    private final LayoutManagerImpl manager;
    private final LayoutPattern pattern;
    private final TabPlayer viewer;
    private final Condition displayCondition;
    private final List<Integer> emptySlots = IntStream.range(1, 81).boxed().collect(Collectors.toList());
    private final Collection<FixedSlot> fixedSlots;
    private final List<ParentGroup> groups = new ArrayList<>();
    private int highestSlot = 0;

    public LayoutView(LayoutManagerImpl manager, LayoutPattern pattern, TabPlayer viewer) {
        this.manager = manager;
        this.viewer = viewer;
        this.pattern = pattern;
        fixedSlots = pattern.getFixedSlots().values();
        displayCondition = pattern.getCondition();
        for (FixedSlot slot : fixedSlots) {
            highestSlot = slot.getSlot() > highestSlot ? slot.getSlot() : highestSlot;
            emptySlots.remove((Integer) slot.getSlot());
        }
        for (GroupPattern group : pattern.getGroups()) {
            emptySlots.removeAll(Arrays.stream(group.slots).boxed().collect(Collectors.toList()));
            groups.add(new ParentGroup(this, group, viewer));
        }
        highestSlot = (highestSlot % 20) == 0 ? highestSlot : highestSlot - (highestSlot % 20) + 20;
        if (ignoreEmptySlots()) {
            emptySlots.removeIf(x -> x > highestSlot);
        }
    }

    public void send() {
        send(null);
    }

    public void send(@Nullable LayoutView previous) {
        if (viewer.getVersion().getMinorVersion() < 8 || viewer.isBedrockPlayer()) return;
        for (ParentGroup group : groups) {
            group.sendSlots();
        }
        for (FixedSlot slot : fixedSlots) {
            if (slot.updateEntry(viewer, previous)) {
                continue;
            }
            viewer.getTabList().removeEntry(slot.getId());
            viewer.getTabList().addEntry(slot.createEntry(viewer));
        }
        // immutable
        final List<Integer> previousEmptySlots = previous == null ? Collections.emptyList() : previous.getEmptySlots();
        for (int slot : emptySlots) {
            // ignore if previous slot was an empty slot as well
            if (previousEmptySlots.contains(slot) && viewer.getTabList().containsEntry(manager.getUUID(slot))) {
                if (ignoreEmptySlots() && slot > highestSlot) {
                    // emptySlots.remove(slot);
                    viewer.getTabList().removeEntry(manager.getUUID(slot));
                }
                continue;
            }
            if (ignoreEmptySlots() && slot > highestSlot) {
                // emptySlots.remove(slot);
                continue;
            }
            viewer.getTabList().removeEntry(manager.getUUID(slot));
            viewer.getTabList().addEntry(new TabList.Entry(
                    manager.getUUID(slot),
                    manager.getConfiguration().direction.getEntryName(viewer, slot, LayoutManagerImpl.isTeamsEnabled()),
                    manager.getSkinManager().getDefaultSkin(slot),
                    true,
                    manager.getConfiguration().emptySlotPing,
                    0,
                    new SimpleComponent("")
            ));
        }
        if (ignoreEmptySlots() && previousEmptySlots != null) {
            for (int slot : previousEmptySlots) {
                if (slot > highestSlot && viewer.getTabList().containsEntry(manager.getUUID(slot))) {
                    viewer.getTabList().removeEntry(manager.getUUID(slot));
                }
            }
        }
        tick();
    }

    public boolean ignoreEmptySlots() {
        return this.manager.getConfiguration().ignoreEmptySlots && this.manager.getConfiguration().hideRealPlayers;
    }

    public void destroy() {
        if (viewer.getVersion().getMinorVersion() < 8 || viewer.isBedrockPlayer()) return;
        for (UUID id : manager.getUuids().values()) {
            viewer.getTabList().removeEntry(id);
        }
    }

    public void tick() {
        if (groups.isEmpty()) {
            return;
        }
        Stream<TabPlayer> str = manager.getSortedPlayers().keySet().stream().filter(
                player -> TAB.getInstance().getPlatform().canSee(viewer, player));
        List<TabPlayer> players = str.collect(Collectors.toList());
        for (ParentGroup group : groups) {
            group.tick(players);
        }
    }

    public PlayerSlot getSlot(@NotNull TabPlayer target) {
        for (ParentGroup group : groups) {
            if (group.getPlayers().containsKey(target)) {
                return group.getPlayers().get(target);
            }
        }
        return null;
    }
}
