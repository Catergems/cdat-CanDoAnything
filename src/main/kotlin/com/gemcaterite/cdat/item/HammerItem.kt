package com.gemcaterite.cdat.item

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

class HammerItem(properties: Properties) : Item(properties) {

    companion object {
        private const val MAX_VEIN = 64

        fun createAttributes(): ItemAttributeModifiers =
            ItemAttributeModifiers.builder()
                .add(
                    Attributes.ATTACK_DAMAGE,
                    AttributeModifier(
                        Identifier.fromNamespaceAndPath("cdat", "hammer_damage"),
                        5.0,
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .add(
                    Attributes.ATTACK_SPEED,
                    AttributeModifier(
                        Identifier.fromNamespaceAndPath("cdat", "hammer_speed"),
                        -3.2,
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .build()
    }

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

        if (!state.isOre()) return true

        val targetBlock = state.block
        val visited = mutableSetOf<BlockPos>()
        val queue = ArrayDeque<BlockPos>()
        queue.add(pos)
        visited.add(pos)

        while (queue.isNotEmpty() && visited.size < MAX_VEIN) {
            val current = queue.removeFirst()
            for (dir in Direction.entries) {
                val neighbor = current.relative(dir)
                if (neighbor in visited) continue
                if (serverLevel.getBlockState(neighbor).block == targetBlock) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }

        for (veinPos in visited) {
            if (veinPos == pos) continue
            serverLevel.destroyBlock(veinPos, true, miner)
            if (!miner.isCreative) {
                stack.hurtAndBreak(1, miner, EquipmentSlot.MAINHAND)
                if (stack.isEmpty) return true
            }
        }

        return true
    }

    override fun getDestroySpeed(stack: ItemStack, state: BlockState): Float = 8.0f

    override fun isCorrectToolForDrops(stack: ItemStack, state: BlockState): Boolean = true

    override fun getDefaultAttributeModifiers(stack: ItemStack): ItemAttributeModifiers = createAttributes()
}
