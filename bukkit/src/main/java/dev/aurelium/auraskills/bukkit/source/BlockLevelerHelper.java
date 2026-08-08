package dev.aurelium.auraskills.bukkit.source;

import dev.aurelium.auraskills.api.source.type.BlockXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.config.Option;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.SeaPickle;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BlockLevelerHelper {

    private final AuraSkills plugin;
    private final Map<BlockXpSource, Material[]> sourceMaterialsCache;

    public BlockLevelerHelper(AuraSkills plugin) {
        this.plugin = plugin;
        this.sourceMaterialsCache = new HashMap<>();
    }

    /**
     * Gets the total number of blocks broken when breaking
     * multi-block plants, including the directly broken one.
     *
     * @param block The directly broken block
     * @param source The XP source
     * @return Number of blocks broken
     */
    public int getBlocksBroken(Block block, BlockXpSource source) {
        Material mat = block.getType();
        boolean checkReplace = checkReplace();
        if (mat == Material.SUGAR_CANE) {
            int num = 1;
            Block aboveBlock = block.getRelative(BlockFace.UP);
            if (aboveBlock.getType() == Material.SUGAR_CANE) {
                if (!plugin.getRegionManager().isPlacedBlock(aboveBlock) || !checkReplace) {
                    num++;
                }
                Block aboveAboveBlock = aboveBlock.getRelative(BlockFace.UP);
                if (aboveAboveBlock.getType() == Material.SUGAR_CANE) {
                    if (!plugin.getRegionManager().isPlacedBlock(aboveAboveBlock) || !checkReplace) {
                        num++;
                    }
                }
            }
            return num;
        } else if (mat == Material.BAMBOO || mat == Material.CACTUS || mat == Material.KELP_PLANT) {
            int num = 1;
            if (checkReplace && plugin.getRegionManager().isPlacedBlock(block)) {
                Block above = block.getRelative(BlockFace.UP);
                if (!sourceMatches(block, source) || plugin.getRegionManager().isPlacedBlock(above)) {
                    return 0;
                }
                num = 0;
            }
            num += getSameBlocksAbove(block.getRelative(BlockFace.UP), source, 0, checkReplace);
            return num;
        }
        return 1;
    }

    public double getStateMultiplier(Block block, BlockXpSource source) {
        if (!source.hasStateMultiplier()) return 1.0;

        if (block.getBlockData() instanceof Ageable ageable) {
            return source.getStateMultiplier("age", ageable.getAge());
        } else if (block.getBlockData() instanceof SeaPickle seaPickle) {
            return source.getStateMultiplier("pickles", seaPickle.getPickles());
        }
        return source.getStateMultiplier("placeholder", 1.0);
    }

    private int getSameBlocksAbove(Block block, BlockXpSource source, int num, boolean checkReplace) {
        Block current = block;
        while (sourceMatches(current, source)) {
            if (checkReplace && plugin.getRegionManager().isPlacedBlock(current)) {
                return num;
            }
            num++;
            current = current.getRelative(BlockFace.UP);
        }
        return num;
    }

    private boolean sourceMatches(Block block, BlockXpSource source) {
        Material blockType = block.getType();
        Material[] materials = sourceMaterialsCache.computeIfAbsent(source, s -> {
            String[] blocks = s.getBlocks();
            Material[] mats = new Material[blocks.length];
            for (int i = 0; i < blocks.length; i++) {
                try {
                    mats[i] = Material.valueOf(blocks[i].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    mats[i] = Material.AIR;
                }
            }
            return mats;
        });
        for (Material mat : materials) {
            if (mat == blockType) return true;
        }
        return false;
    }

    private boolean checkReplace() {
        return plugin.configBoolean(Option.CHECK_BLOCK_REPLACE_ENABLED);
    }

}
