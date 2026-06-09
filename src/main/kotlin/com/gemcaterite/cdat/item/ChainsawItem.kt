package com.gemcaterite.cdat.item

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

class ChainsawItem(properties: Properties) : Item(properties) {

    companion object {
        private const val MAX_LOGS = 256

        private fun BlockState.isLog() = this.`is`(BlockTags.LOGS)
        private fun BlockState.isLeaf() = this.`is`(BlockTags.LEAVES)
    }

    override fun mineBlock(stack: ItemStack, level: Level, state: BlockState, pos: BlockPos, miner: LivingEntity): Boolean {
        if (level.isClientSide || miner !is Player) return true
        val serverLevel = level as? ServerLevel ?: return true
        if (!state.isLog()) return true

        // BFS — find all connected logs
        val logs = mutableSetOf<BlockPos>()
        val queue = ArrayDeque<BlockPos>()
        queue.add(pos); logs.add(pos)

        while (queue.isNotEmpty() && logs.size < MAX_LOGS) {
            val current = queue.removeFirst()
            for (dir in Direction.entries) {
                val neighbor = current.relative(dir)
                if (neighbor in logs) continue
                if (serverLevel.getBlockState(neighbor).isLog()) {
                    logs.add(neighbor); queue.add(neighbor)
                }
            }
        }

        // Collect leaves within 6 blocks of any log
        val leaves = mutableSetOf<BlockPos>()
        for (logPos in logs) {
            for (dx in -6..6) for (dy in -6..6) for (dz in -6..6) {
                val check = logPos.offset(dx, dy, dz)
                if (check in leaves || check in logs) continue
                if (serverLevel.getBlockState(check).isLeaf()) leaves.add(check)
            }
        }

        // Break logs (skip origin — vanilla handles it)
        for (logPos in logs) {
            if (logPos == pos) continue
            val logState = serverLevel.getBlockState(logPos)
            val drops = Block.getDrops(logState, serverLevel, logPos, serverLevel.getBlockEntity(logPos), miner, ItemStack.EMPTY)
            serverLevel.removeBlock(logPos, false)
            for (drop in drops) {
                serverLevel.addFreshEntity(ItemEntity(serverLevel, logPos.x + 0.5, logPos.y + 0.5, logPos.z + 0.5, drop))
            }
            if (!miner.isCreative) {
                stack.hurtAndBreak(1, miner, EquipmentSlot.MAINHAND)
                if (stack.isEmpty) return true
            }
        }

        // Break leaves — drop saplings + sticks
        val rand = serverLevel.random
        for (leafPos in leaves) {
            val leafState = serverLevel.getBlockState(leafPos)
            if (!leafState.isLeaf()) continue
            serverLevel.removeBlock(leafPos, false)

            val sapling = getSaplingForLeaves(leafState)
            if (sapling != null && rand.nextFloat() < 0.05f) {
                serverLevel.addFreshEntity(ItemEntity(serverLevel, leafPos.x + 0.5, leafPos.y + 0.5, leafPos.z + 0.5, ItemStack(sapling)))
            }
            if (rand.nextFloat() < 0.02f) {
                serverLevel.addFreshEntity(ItemEntity(serverLevel, leafPos.x + 0.5, leafPos.y + 0.5, leafPos.z + 0.5, ItemStack(Items.STICK, 1 + rand.nextInt(2))))
            }
        }

        return true
    }

    private fun getSaplingForLeaves(state: BlockState): Item? {
        val blockId = BuiltInRegistries.BLOCK.getKey(state.block).path
        val saplingId = when {
            blockId.contains("dark_oak")  -> "dark_oak_sapling"
            blockId.contains("oak")       -> "oak_sapling"
            blockId.contains("birch")     -> "birch_sapling"
            blockId.contains("spruce")    -> "spruce_sapling"
            blockId.contains("jungle")    -> "jungle_sapling"
            blockId.contains("acacia")    -> "acacia_sapling"
            blockId.contains("cherry")    -> "cherry_sapling"
            blockId.contains("mangrove")  -> "mangrove_propagule"
            blockId.contains("azalea")    -> "azalea"
            else -> null
        } ?: return null
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(saplingId))
    }

    override fun getDestroySpeed(stack: ItemStack, state: BlockState): Float =
        if (state.isLog() || state.isLeaf()) 12.0f else 1.0f

    override fun isCorrectToolForDrops(stack: ItemStack, state: BlockState): Boolean =
        state.isLog() || state.isLeaf()
}
