package xyz.vp.moosh.blower;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.*;

public final class Blower extends JavaPlugin {

    private static final Set<Material> LOG_TYPES = EnumSet.of(
            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG,
            Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
            Material.MANGROVE_LOG, Material.CHERRY_LOG
    );

    private static final Set<Material> LEAF_TYPES = EnumSet.of(
            Material.OAK_LEAVES, Material.BIRCH_LEAVES, Material.SPRUCE_LEAVES,
            Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES,
            Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES, Material.AZALEA_LEAVES,
            Material.FLOWERING_AZALEA_LEAVES
    );

    @Override
    public void onEnable() {
        getLogger().info("Tree Blower Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Tree Blower Plugin has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("blow")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }

            Player player = (Player) sender;
            Location playerLoc = player.getLocation();

            // Find the closest tree within 50 blocks
            Block closestLog = findClosestTree(playerLoc, 50);

            if (closestLog == null) {
                player.sendMessage(Component.text("No trees found nearby!", NamedTextColor.RED));
                return true;
            }

            // Remove the tree
            removeTree(closestLog);

            double distance = playerLoc.distance(closestLog.getLocation());
            player.sendMessage(Component.text(String.format("💨 WHOOSH! Tree blown away! (%.1f blocks away)", distance), NamedTextColor.GREEN));

            return true;
        }

        if (command.getName().equalsIgnoreCase("blowhelp")) {
            sender.sendMessage(Component.text("=== Tree Blower Plugin Help ===", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("A tree removing utility mod made by ", NamedTextColor.GREEN)
                    .append(Component.text("Moosh ", NamedTextColor.AQUA))
                    .append(Component.text("(Discord: mooshbrixa)", NamedTextColor.GRAY)));
            sender.sendMessage(Component.empty());
            sender.sendMessage(Component.text("Commands:", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/blow ", NamedTextColor.WHITE)
                    .append(Component.text("- Removes the closest tree to you (within 50 blocks)", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/blowhelp ", NamedTextColor.WHITE)
                    .append(Component.text("- Shows this help message", NamedTextColor.GRAY)));
            sender.sendMessage(Component.empty());
            sender.sendMessage(Component.text("Note: Only OPs or players with permission can use /blow", NamedTextColor.GRAY));
            return true;
        }

        return false;
    }

    private Block findClosestTree(Location center, int radius) {
        Block closestLog = null;
        double closestDistance = Double.MAX_VALUE;

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        // Search in a cube around the player
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = Math.max(centerY - radius, center.getWorld().getMinHeight());
                 y <= Math.min(centerY + radius, center.getWorld().getMaxHeight()); y++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {

                    Block block = center.getWorld().getBlockAt(x, y, z);

                    if (LOG_TYPES.contains(block.getType())) {
                        // Check if this log is part of a tree (has leaves nearby or above)
                        if (isPartOfTree(block)) {
                            double distance = center.distance(block.getLocation());
                            if (distance < closestDistance) {
                                closestDistance = distance;
                                closestLog = block;
                            }
                        }
                    }
                }
            }
        }

        return closestLog;
    }

    private boolean isPartOfTree(Block logBlock) {
        // Check for leaves within a 7x7x7 area around the log
        Location logLoc = logBlock.getLocation();

        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block checkBlock = logLoc.getWorld().getBlockAt(
                            logLoc.getBlockX() + x,
                            logLoc.getBlockY() + y,
                            logLoc.getBlockZ() + z
                    );

                    if (LEAF_TYPES.contains(checkBlock.getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void removeTree(Block startLog) {
        Set<Block> toRemove = new HashSet<>();
        Queue<Block> toCheck = new LinkedList<>();
        Set<Block> checked = new HashSet<>();

        toCheck.offer(startLog);

        // Find all connected logs and leaves
        while (!toCheck.isEmpty()) {
            Block current = toCheck.poll();

            if (checked.contains(current)) {
                continue;
            }
            checked.add(current);

            Material type = current.getType();

            if (LOG_TYPES.contains(type) || LEAF_TYPES.contains(type)) {
                toRemove.add(current);

                // Check all 26 adjacent blocks (including diagonals)
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x == 0 && y == 0 && z == 0) continue;

                            Block adjacent = current.getRelative(x, y, z);
                            if (!checked.contains(adjacent)) {
                                Material adjType = adjacent.getType();
                                if (LOG_TYPES.contains(adjType) || LEAF_TYPES.contains(adjType)) {
                                    toCheck.offer(adjacent);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Remove all blocks that are part of the tree
        for (Block block : toRemove) {
            block.setType(Material.AIR);
        }
    }
}