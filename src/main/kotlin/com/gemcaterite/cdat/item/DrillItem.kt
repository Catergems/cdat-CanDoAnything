package com.gemcaterite.cdat.item

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

class DrillItem(properties: Properties) : Item(properties) {

    private fun BlockState.isOre(): Boolean =
        this.`is`(BlockTags.COAL_ORES) ||
        this.`is`(BlockTags.IRON_ORES) ||
        this.`is`(BlockTags.GOLD_ORES) ||
        this.`is`(BlockTags.DIAMOND_ORES) ||
        this.`is`(BlockTags.EMERALD_ORES) ||
        this.`is`(BlockTags.LAPIS_ORES) ||
        this.`is`(BlockTags.REDSTONE_ORES) ||
        this.`is`(BlockTags.COPPER_ORES)

    override fun mineBlock(stack: ItemStack, level: Level, state: BlockState, pos: BlockPos, miner: LivingEntity): Boolean {
        if (level.isClientSide || miner !is Player) return true
        val serverLevel = level as? ServerLevel ?: return true

        val hitFace = (miner.pick(5.0, 0f, false) as? BlockHitResult)
            ?.takeIf { it.type == HitResult.Type.BLOCK && it.blockPos == pos }
            ?.direction
            ?: miner.direction

        val (side1, side2) = when (hitFace.axis) {
            Direction.Axis.X -> Direction.NORTH to Direction.UP
            Direction.Axis.Z -> Direction.EAST  to Direction.UP
            Direction.Axis.Y -> Direction.EAST  to Direction.NORTH
        }

        for (o1 in -1..1) {
            for (o2 in -1..1) {
                if (o1 == 0 && o2 == 0) continue

                val target = pos.relative(side1, o1).relative(side2, o2)
                val targetState = serverLevel.getBlockState(target)

                if (targetState.isAir) continue
                if (targetState.`is`(Blocks.BEDROCK)) continue
                if (targetState.isOre()) continue  // leave ores for the hammer

                serverLevel.destroyBlock(target, true, miner)

                if (!miner.isCreative) {
                    stack.hurtAndBreak(1, miner, EquipmentSlot.MAINHAND)
                    if (stack.isEmpty) return true
                }
            }
        }

        return true
    }

    override fun getDestroySpeed(stack: ItemStack, state: BlockState): Float = 8.0f

    override fun isCorrectToolForDrops(stack: ItemStack, state: BlockState): Boolean = true
}
