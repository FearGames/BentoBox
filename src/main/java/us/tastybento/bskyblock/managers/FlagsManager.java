package us.tastybento.bskyblock.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.lists.Flags;

/**
 * @author Poslovitch
 * @author tastybento
 */
public class FlagsManager {

    private BSkyBlock plugin;
    private List<Flag> flags = new ArrayList<>();

    /**
     * Stores the flag listeners that have already been registered into Bukkit's API to avoid duplicates.
     * Value is true if the listener has been registered already
     */
    private Map<Listener, Boolean> registeredListeners = new HashMap<>();

    public FlagsManager(BSkyBlock plugin) {
        this.plugin = plugin;

        // Register default flags
        Flags.values().forEach(this::registerFlag);
    }

    /**
     * Register a new flag with BSkyBlock
     * @param flag flag to be registered
     * @return true if successfully registered, false if not, e.g., because one with the same ID already exists
     */
    public boolean registerFlag(Flag flag) {
        // Check in case the flag id or icon already exists
        for (Flag fl : flags) {
            if (fl.getID().equals(flag.getID()) || fl.getIcon().equals(flag.getIcon())) {
                return false;
            }
        }
        flags.add(flag);
        // If there is a listener which is not already registered, register it into Bukkit if the plugin is fully loaded
        flag.getListener().ifPresent(this::registerListener);
        return true;
    }

    /**
     * Register any unregistered listeners - called after the plugin is fully loaded
     */
    public void registerListeners() {
        registeredListeners.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).forEach(this::registerListener);
    }

    /**
     * Tries to register a listener if the plugin is loaded
     * @param l
     */
    private void registerListener(Listener l) {
        registeredListeners.putIfAbsent(l, false);
        if (BSkyBlock.getInstance().isLoaded() && !registeredListeners.get(l)) {
            Bukkit.getServer().getPluginManager().registerEvents(l, plugin);
            registeredListeners.put(l, true);
        }
    }

    /**
     * @return list of all flags
     */
    public List<Flag> getFlags() {
        return flags;
    }

    /**
     * Get flag by ID
     * @param id unique id for this flag
     * @return Flag or null if not known
     */
    public Flag getFlagByID(String id) {
        return flags.stream().filter(flag -> flag.getID().equals(id)).findFirst().orElse(null);
    }
}
