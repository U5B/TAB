package me.neznamy.tab.shared.features.bossbar;

import lombok.Getter;
import lombok.NonNull;
import me.neznamy.tab.api.bossbar.BarColor;
import me.neznamy.tab.api.bossbar.BarStyle;
import me.neznamy.tab.api.bossbar.BossBar;
import me.neznamy.tab.shared.Property;
import me.neznamy.tab.shared.TAB;
import me.neznamy.tab.shared.TabConstants;
import me.neznamy.tab.shared.config.files.config.BossBarConfiguration.BossBarDefinition;
import me.neznamy.tab.shared.cpu.ThreadExecutor;
import me.neznamy.tab.shared.features.types.CustomThreaded;
import me.neznamy.tab.shared.features.types.RefreshableFeature;
import me.neznamy.tab.shared.placeholders.conditions.Condition;
import me.neznamy.tab.shared.platform.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Class representing a BossBar from configuration
 */
public class BossBarLine implements BossBar {

    private final BossBarManagerImpl manager;

    //BossBar name
    @Getter private final String name;

    //display condition
    private final Condition displayCondition;

    //uuid
    @Getter private final UUID uniqueId = UUID.randomUUID();

    //BossBar style
    @Getter private String style;

    //BossBar color
    @Getter private String color;

    //BossBar title
    @Getter private String title;

    //BossBar progress
    @Getter private String progress;

    @Getter private final boolean announcementBar;

    //set of players seeing this BossBar
    private final Set<TabPlayer> players = new HashSet<>();

    //refreshers
    private final TextRefresher textRefresher;
    private final ProgressRefresher progressRefresher;
    private final ColorRefresher colorRefresher;
    private final StyleRefresher styleRefresher;

    //property names
    private final String propertyTitle = Property.randomName();
    private final String propertyProgress = Property.randomName();
    private final String propertyColor = Property.randomName();
    private final String propertyStyle = Property.randomName();

    /**
     * Constructs new instance with given parameters
     *
     * @param   manager
     *          BossBar manager to count sent packets for
     * @param   name
     *          name of BossBar
     * @param   configuration
     *          Boss bar configuration
     */
    public BossBarLine(@NonNull BossBarManagerImpl manager, @NonNull String name, @NonNull BossBarDefinition configuration) {
        this.manager = manager;
        this.name = name;
        displayCondition = Condition.getCondition(configuration.displayCondition);
        if (displayCondition != null) {
            manager.addUsedPlaceholder(TabConstants.Placeholder.condition(displayCondition.getName()));
        }
        color = configuration.color;
        style = configuration.style;
        title = configuration.text;
        progress = configuration.progress;
        announcementBar = configuration.announcementOnly;
        TAB.getInstance().getFeatureManager().registerFeature(TabConstants.Feature.bossBarTitle(name),
                textRefresher = new TextRefresher());
        TAB.getInstance().getFeatureManager().registerFeature(TabConstants.Feature.bossBarProgress(name),
                progressRefresher = new ProgressRefresher());
        TAB.getInstance().getFeatureManager().registerFeature(TabConstants.Feature.bossBarColor(name),
                colorRefresher = new ColorRefresher());
        TAB.getInstance().getFeatureManager().registerFeature(TabConstants.Feature.bossBarStyle(name),
                styleRefresher = new StyleRefresher());
    }

    /**
     * Returns true if condition is null or is met, false otherwise.
     *
     * @param   p
     *          player to check condition for
     * @return  true if met, false if not
     */
    public boolean isConditionMet(@NonNull TabPlayer p) {
        if (displayCondition == null) return true;
        return displayCondition.isMet(p);
    }

    /**
     * Parses string into color and returns it. If parsing failed, PURPLE is returned.
     *
     * @param   player
     *          Player to parse color for
     * @param   color
     *          string to parse
     * @return  parsed color
     */
    @NotNull
    public BarColor parseColor(@NotNull TabPlayer player, @NonNull String color) {
        try {
            return BarColor.valueOf(color);
        } catch (IllegalArgumentException e) {
            TAB.getInstance().getConfigHelper().runtime().invalidBossBarProperty(
                    this,
                    color,
                    player.getProperty(propertyColor).getCurrentRawValue(),
                    player,
                    "color",
                    "one of the pre-defined values " + Arrays.toString(BarColor.values())
            );
            return BarColor.PURPLE;
        }
    }

    /**
     * Parses string into style and returns it. If parsing failed, PROGRESS is returned.
     *
     * @param   player
     *          Player to parse style for
     * @param   style
     *          string to parse
     * @return  parsed style
     */
    @NotNull
    public BarStyle parseStyle(@NotNull TabPlayer player, @NonNull String style) {
        try {
            return BarStyle.valueOf(style);
        } catch (IllegalArgumentException e) {
            TAB.getInstance().getConfigHelper().runtime().invalidBossBarProperty(
                    this,
                    style,
                    player.getProperty(propertyStyle).getCurrentRawValue(),
                    player,
                    "style",
                    "one of the pre-defined values " + Arrays.toString(BarStyle.values())
            );
            return BarStyle.PROGRESS;
        }
    }

    /**
     * Parses string into progress and returns it. If parsing failed, 100 is returned instead and
     * error message is printed into error log
     *
     * @param   player
     *          Player to parse the value for
     * @param   progress
     *          string to parse
     * @return  parsed progress
     */
    public float parseProgress(@NotNull TabPlayer player, @NotNull String progress) {
        try {
            float value = Float.parseFloat(progress);
            if (value < 0) value = 0;
            if (value > 100) value = 100;
            return value;
        } catch (NumberFormatException e) {
            TAB.getInstance().getConfigHelper().runtime().invalidBossBarProperty(
                    this,
                    progress,
                    player.getProperty(propertyProgress).getCurrentRawValue(),
                    player,
                    "progress",
                    "a number between 0 and 100"
            );
            return 100;
        }
    }

    /**
     * Removes player from set of players.
     *
     * @param   player
     *          Player to remove
     */
    public void removePlayerRaw(@NotNull TabPlayer player) {
        players.remove(player);
    }

    // ------------------
    // API Implementation
    // ------------------

    @Override
    public void setTitle(@NonNull String title) {
        if (this.title.equals(title)) return;
        this.title = title;
        for (TabPlayer p : players) {
            p.setProperty(textRefresher, propertyTitle, title);
            p.getBossBar().update(uniqueId, manager.getCache().get(p.getProperty(propertyTitle).get()));
        }
    }

    @Override
    public void setProgress(@NonNull String progress) {
        if (this.progress.equals(progress)) return;
        this.progress = progress;
        for (TabPlayer p : players) {
            p.setProperty(progressRefresher, propertyProgress, progress);
            p.getBossBar().update(uniqueId, parseProgress(p, p.getProperty(propertyProgress).get())/100);
        }
    }

    @Override
    public void setProgress(float progress) {
        setProgress(String.valueOf(progress));
    }

    @Override
    public void setColor(@NonNull String color) {
        if (this.color.equals(color)) return;
        this.color = color;
        for (TabPlayer p : players) {
            p.setProperty(colorRefresher, propertyColor, color);
            p.getBossBar().update(uniqueId, parseColor(p, p.getProperty(propertyColor).get()));
        }
    }

    @Override
    public void setColor(@NonNull BarColor color) {
        setColor(color.toString());
    }

    @Override
    public void setStyle(@NonNull String style) {
        if (this.style.equals(style)) return;
        this.style = style;
        for (TabPlayer p : players) {
            p.setProperty(styleRefresher, propertyColor, style);
            p.getBossBar().update(uniqueId, parseStyle(p, p.getProperty(propertyStyle).get()));
        }
    }

    @Override
    public void setStyle(@NonNull BarStyle style) {
        setStyle(style.toString());
    }

    @Override
    public void addPlayer(@NonNull me.neznamy.tab.api.TabPlayer p) {
        TabPlayer player = (TabPlayer) p;
        if (player.bossbarData.visibleBossBars.contains(this)) return;
        player.setProperty(textRefresher, propertyTitle, title);
        player.setProperty(progressRefresher, propertyProgress, progress);
        player.setProperty(colorRefresher, propertyColor, color);
        player.setProperty(styleRefresher, propertyStyle, style);
        player.getBossBar().create(
                uniqueId,
                manager.getCache().get(player.getProperty(propertyTitle).updateAndGet()),
                parseProgress(player, player.getProperty(propertyProgress).updateAndGet())/100,
                parseColor(player, player.getProperty(propertyColor).updateAndGet()),
                parseStyle(player, player.getProperty(propertyStyle).updateAndGet())
        );
        players.add(player);
        player.bossbarData.visibleBossBars.add(this);
    }

    @Override
    public void removePlayer(@NonNull me.neznamy.tab.api.TabPlayer p) {
        TabPlayer player = (TabPlayer) p;
        if (!player.bossbarData.visibleBossBars.contains(this)) return;
        players.remove(player);
        player.bossbarData.visibleBossBars.remove(this);
        player.getBossBar().remove(uniqueId);
    }

    @Override
    @NotNull
    public List<me.neznamy.tab.api.TabPlayer> getPlayers() {
        return new ArrayList<>(players);
    }

    @Override
    public boolean containsPlayer(@NonNull me.neznamy.tab.api.TabPlayer player) {
        return ((TabPlayer)player).bossbarData.visibleBossBars.contains(this);
    }

    private class TextRefresher extends RefreshableFeature implements CustomThreaded {

        @Override
        public void refresh(@NotNull TabPlayer refreshed, boolean force) {
            if (!refreshed.bossbarData.visibleBossBars.contains(BossBarLine.this)) return;
            refreshed.getBossBar().update(uniqueId, manager.getCache().get(refreshed.getProperty(propertyTitle).updateAndGet()));
        }

        @Override
        @NotNull
        public ThreadExecutor getCustomThread() {
            return manager.getCustomThread();
        }

        @NotNull
        @Override
        public String getFeatureName() {
            return "BossBar";
        }

        @NotNull
        @Override
        public String getRefreshDisplayName() {
            return "Updating text";
        }
    }

    private class ProgressRefresher extends RefreshableFeature implements CustomThreaded {

        @Override
        public void refresh(@NotNull TabPlayer refreshed, boolean force) {
            if (!refreshed.bossbarData.visibleBossBars.contains(BossBarLine.this)) return;
            refreshed.getBossBar().update(uniqueId, parseProgress(refreshed, refreshed.getProperty(propertyProgress).updateAndGet())/100);
        }

        @Override
        @NotNull
        public ThreadExecutor getCustomThread() {
            return manager.getCustomThread();
        }

        @NotNull
        @Override
        public String getFeatureName() {
            return "BossBar";
        }

        @NotNull
        @Override
        public String getRefreshDisplayName() {
            return "Updating progress";
        }
    }

    private class ColorRefresher extends RefreshableFeature implements CustomThreaded {

        @Override
        public void refresh(@NotNull TabPlayer refreshed, boolean force) {
            if (!refreshed.bossbarData.visibleBossBars.contains(BossBarLine.this)) return;
            refreshed.getBossBar().update(uniqueId, parseColor(refreshed, refreshed.getProperty(propertyColor).updateAndGet()));
        }

        @Override
        @NotNull
        public ThreadExecutor getCustomThread() {
            return manager.getCustomThread();
        }

        @NotNull
        @Override
        public String getFeatureName() {
            return "BossBar";
        }

        @NotNull
        @Override
        public String getRefreshDisplayName() {
            return "Updating color";
        }
    }

    private class StyleRefresher extends RefreshableFeature implements CustomThreaded {

        @Override
        public void refresh(@NotNull TabPlayer refreshed, boolean force) {
            if (!refreshed.bossbarData.visibleBossBars.contains(BossBarLine.this)) return;
            refreshed.getBossBar().update(uniqueId, parseStyle(refreshed, refreshed.getProperty(propertyStyle).updateAndGet()));
        }

        @Override
        @NotNull
        public ThreadExecutor getCustomThread() {
            return manager.getCustomThread();
        }

        @NotNull
        @Override
        public String getFeatureName() {
            return "BossBar";
        }

        @NotNull
        @Override
        public String getRefreshDisplayName() {
            return "Updating style";
        }
    }
}