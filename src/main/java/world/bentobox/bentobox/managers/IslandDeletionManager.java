package world.bentobox.bentobox.managers;

import java.util.*;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.scheduler.BukkitRunnable;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.events.island.IslandDeleteChunksEvent;
import world.bentobox.bentobox.api.events.island.IslandDeletedEvent;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.IslandDeletion;
import world.bentobox.bentobox.util.DeleteIslandChunks;
import world.bentobox.bentobox.util.Util;

/**
 * Listens for island deletions and adds them to the database. Removes them when the island is deleted.
 * @author tastybento
 * @since 1.1
 */
public class IslandDeletionManager implements Listener {

    private BentoBox plugin;
    /**
     * Queue of islands to delete
     */
    private Database<IslandDeletion> handler;
    private Set<Location> inDeletion;

    private Queue<IslandDeletion> startupDeletionIslands = new LinkedList<>();

    public IslandDeletionManager(BentoBox plugin) {
        this.plugin = plugin;
        handler = new Database<>(plugin, IslandDeletion.class);
        inDeletion = new HashSet<>();
    }

    /**
     * When BentoBox is fully loaded, load the islands that still need to be deleted and kick them off
     * @param e BentoBox Ready event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBentoBoxReady(BentoBoxReadyEvent e) {
        // Load list of islands that were mid deletion and delete them
        List<IslandDeletion> toBeDeleted = handler.loadObjects();
        //List<IslandDeletion> toBeRemoved = new ArrayList<>();
        if (!toBeDeleted.isEmpty()) {
            plugin.log("There are " + toBeDeleted.size() + " islands pending deletion.");
            toBeDeleted.forEach(di -> {
                if (di.getLocation() == null || di.getLocation().getWorld() == null) {
                    plugin.logError("Island queued for deletion refers to a non-existant game world. Skipping...");
                    //toBeRemoved.add(di);
                } else {
                    plugin.log("Resuming deletion of island at " + di.getLocation().getWorld().getName() + " " + Util.xyz(di.getLocation().toVector()));
                    inDeletion.add(di.getLocation());
                    startupDeletionIslands.add(di);
                }
            });
            if (!startupDeletionIslands.isEmpty()) {
                int total = startupDeletionIslands.size();
                final int[] index = {0};
                final DeleteIslandChunks[] current = new DeleteIslandChunks[1];
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (current[0] != null && !current[0].isCompleted()) {
                            // Still running
                            return;
                        }
                        IslandDeletion deletion = startupDeletionIslands.poll();
                        index[0]++;
                        if (deletion == null) {
                            startupDeletionIslands = null; // Free memory
                            cancel();
                            plugin.log("Completed startup island deletion task!");
                            return;
                        }
                        plugin.log("Deleting island " + deletion.getUniqueId() + " (" + index[0] + "/" + total + ")");
                        current[0] = new DeleteIslandChunks(plugin, deletion);
                    }
                }.runTaskTimer(plugin, 0, 1L);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIslandDelete(IslandDeleteChunksEvent e) {
        // Store location
        inDeletion.add(e.getDeletedIslandInfo().getLocation());
        // Save to database
        handler.saveObjectAsync(e.getDeletedIslandInfo());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIslandDeleted(IslandDeletedEvent e) {
        // Delete
        inDeletion.remove(e.getDeletedIslandInfo().getLocation());
        // Delete from database
        handler.deleteID(e.getDeletedIslandInfo().getUniqueId());
    }

    /**
     * Check if an island location is in deletion
     * @param location - center of location
     * @return true if island is in the process of being deleted
     */
    public boolean inDeletion(Location location) {
        return inDeletion.contains(location);
    }
}
