package me.neznamy.tab.shared.features.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import me.neznamy.tab.shared.config.files.config.LayoutConfiguration.LayoutDefinition.GroupPattern;
import me.neznamy.tab.shared.placeholders.conditions.Condition;
import me.neznamy.tab.shared.platform.TabList;
import me.neznamy.tab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParentGroup {

    @NotNull private final LayoutView layout;
    @Nullable private final Condition condition;
    @Getter private final int[] slots;
    private final TabPlayer viewer;
    @Getter private final Map<Integer, PlayerSlot> playerSlots = new HashMap<>();
    @Getter private final Map<TabPlayer, PlayerSlot> players = new HashMap<>();

    public ParentGroup(@NotNull LayoutView layout, @NotNull GroupPattern pattern, @NotNull TabPlayer viewer) {
        this.layout = layout;
        condition = Condition.getCondition(pattern.condition);
        slots = pattern.slots;
        this.viewer = viewer;
        for (int slot : slots) {
            playerSlots.put(slot, new PlayerSlot(slot, layout, layout.getManager().getUUID(slot)));
        }
    }

    public void tick(@NotNull List<TabPlayer> remainingPlayers) {
        players.clear();
        List<TabPlayer> meetingCondition = new ArrayList<>();
        for (TabPlayer p : remainingPlayers) {
            if (condition == null || condition.isMet(p)) meetingCondition.add(p);
        }
        remainingPlayers.removeAll(meetingCondition);
        for (int index = 0; index < slots.length; index++) {
            int slot = slots[index];
            if (layout.getManager().getConfiguration().remainingPlayersTextEnabled && index == slots.length - 1 && playerSlots.size() < meetingCondition.size()) {
                playerSlots.get(slot).setText(String.format(layout.getManager().getConfiguration().remainingPlayersText, meetingCondition.size() - playerSlots.size() + 1));
                break;
            }
            if (meetingCondition.size() > index) {
                TabPlayer p = meetingCondition.get(index);
                playerSlots.get(slot).setPlayer(p);
                players.put(p, playerSlots.get(slot));
            } else {
                playerSlots.get(slot).setText("");
            }
        }
    }

    public void sendSlots() {
        for (PlayerSlot s : playerSlots.values()) {
            TabList.Entry entry = s.getSlot(viewer);
            viewer.getTabList().removeEntry(entry.getUniqueId());
            viewer.getTabList().addEntry(entry);
        }
    }
}