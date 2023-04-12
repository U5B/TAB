package me.neznamy.tab.platforms.bungeecord;

import lombok.Getter;
import lombok.NonNull;
import me.neznamy.tab.shared.platform.bossbar.PlatformBossBar;
import me.neznamy.tab.api.ProtocolVersion;
import me.neznamy.tab.shared.TabConstants;
import me.neznamy.tab.shared.chat.IChatBaseComponent;
import me.neznamy.tab.shared.platform.tablist.TabList;
import me.neznamy.tab.shared.util.ComponentCache;
import me.neznamy.tab.platforms.bungeecord.tablist.BungeeTabList1_19_3;
import me.neznamy.tab.platforms.bungeecord.tablist.BungeeTabList1_7;
import me.neznamy.tab.platforms.bungeecord.tablist.BungeeTabList1_8;
import me.neznamy.tab.shared.platform.PlatformScoreboard;
import me.neznamy.tab.shared.TAB;
import me.neznamy.tab.shared.proxy.ProxyTabPlayer;
import me.neznamy.tab.shared.util.ReflectionUtils;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.Property;
import net.md_5.bungee.protocol.Protocol;

import java.lang.reflect.Method;

/**
 * TabPlayer implementation for BungeeCord
 */
public class BungeeTabPlayer extends ProxyTabPlayer {

    /** Component cache to save CPU when creating components */
    private static final ComponentCache<IChatBaseComponent, BaseComponent[]> componentCache = new ComponentCache<>(10000,
            (component, clientVersion) -> ComponentSerializer.parse(component.toString(clientVersion)));

    /** Inaccessible bungee internals */
    private static Object directionData;
    private static Method getId;

    static {
        try {
            directionData = ReflectionUtils.setAccessible(Protocol.class.getDeclaredField("TO_CLIENT")).get(Protocol.GAME);
            getId = ReflectionUtils.setAccessible(directionData.getClass().getDeclaredMethod("getId", Class.class, int.class));
        } catch (ReflectiveOperationException e) {
            TAB.getInstance().getErrorManager().criticalError("Failed to initialize bungee internal fields", e);
        }
    }

    /** Player's scoreboard */
    @Getter private final PlatformScoreboard<BungeeTabPlayer> scoreboard = new BungeeScoreboard(this);

    /** Player's tablist based on version */
    private final TabList tabList1_7 = new BungeeTabList1_7(this);
    private final TabList tabList1_8 = new BungeeTabList1_8(this);
    private final TabList tabList1_19_3 = new BungeeTabList1_19_3(this);

    @Getter private final PlatformBossBar bossBar = new BungeeBossBar(this);

    /**
     * Constructs new instance for given player
     *
     * @param   p
     *          BungeeCord player
     */
    public BungeeTabPlayer(ProxiedPlayer p) {
        super(p, p.getUniqueId(), p.getName(), p.getServer() != null ? p.getServer().getInfo().getName() : "-", -1);
    }

    @Override
    public boolean hasPermission0(@NonNull String permission) {
        return getPlayer().hasPermission(permission);
    }

    @Override
    public int getPing() {
        return getPlayer().getPing();
    }

    public void sendPacket(@NonNull Object nmsPacket) {
        getPlayer().unsafe().sendPacket((DefinedPacket) nmsPacket);
    }

    @Override
    public void sendMessage(IChatBaseComponent message) {
        getPlayer().sendMessage(componentCache.get(message, getVersion()));
    }

    @Override
    public TabList.Skin getSkin() {
        LoginResult loginResult = ((InitialHandler)getPlayer().getPendingConnection()).getLoginProfile();
        if (loginResult == null) return null;
        Property[] properties = loginResult.getProperties();
        if (properties == null || properties.length == 0) return null;
        return new TabList.Skin(properties[0].getValue(), properties[0].getSignature());
    }

    @Override
    public ProxiedPlayer getPlayer() {
        return (ProxiedPlayer) player;
    }

    /**
     * Returns packet ID for this player of provided packet class
     *
     * @param   clazz
     *          packet class
     * @return  packet ID
     */
    public int getPacketId(@NonNull Class<? extends DefinedPacket> clazz) {
        try {
            return (int) getId.invoke(directionData, clazz, getPlayer().getPendingConnection().getVersion());
        } catch (ReflectiveOperationException e) {
            TAB.getInstance().getErrorManager().printError("Failed to get packet id for packet " + clazz + " with client version " + getPlayer().getPendingConnection().getVersion(), e);
            return -1;
        }
    }

    /**
     * If ViaVersion is installed on BungeeCord, it changes protocol to match version
     * of server to which player is connected to. For that reason, we need to retrieve
     * the field more often than just on join.
     *
     * @return  Player's current protocol version
     */
    @Override
    public ProtocolVersion getVersion() {
        return ProtocolVersion.fromNetworkId(getPlayer().getPendingConnection().getVersion());
    }

    @Override
    public boolean isVanished() {
        try {
            if (ProxyServer.getInstance().getPluginManager().getPlugin(TabConstants.Plugin.PREMIUM_VANISH) != null &&
                    (boolean) Class.forName("de.myzelyam.api.vanish.BungeeVanishAPI").getMethod("isInvisible", ProxiedPlayer.class).invoke(null, getPlayer())) return true;
        } catch (Exception e) {
            TAB.getInstance().getErrorManager().printError("PremiumVanish v" + TAB.getInstance().getPlatform().getPluginVersion(TabConstants.Plugin.PREMIUM_VANISH) +
                    " generated an error when retrieving vanish status of " + getName(), e);
        }
        return super.isVanished();
    }

    @Override
    public boolean isOnline() {
        return getPlayer().isConnected();
    }

    @Override
    public int getGamemode() {
        return ((UserConnection)player).getGamemode();
    }

    @Override
    public void setPlayerListHeaderFooter(@NonNull IChatBaseComponent header, @NonNull IChatBaseComponent footer) {
        getPlayer().setTabHeader(componentCache.get(header, getVersion()), componentCache.get(footer, getVersion()));
    }

    @Override
    public TabList getTabList() {
        return getVersion().getNetworkId() >= ProtocolVersion.V1_19_3.getNetworkId() ?
                tabList1_19_3 : getVersion().getMinorVersion() >= 8 ? tabList1_8 : tabList1_7;
    }

    @Override
    public void sendPluginMessage(byte[] message) {
        if (getPlayer().getServer() == null) {
            TAB.getInstance().getErrorManager().printError("Skipped plugin message send to " + getName() + ", because player is not" +
                    "connected to any server (message=" + new String(message) + ")");
            return;
        }
        getPlayer().getServer().sendData(TabConstants.PLUGIN_MESSAGE_CHANNEL_NAME, message);
    }
}