package com.gemcaterite.cdat.item

import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemAttributeModifiers

class LeafHatItem(properties: Properties) : Item(properties) {

    companion object {
        val FALL_REDUCTION_ID = Identifier.fromNamespaceAndPath("cdat", "leaf_hat_fall")

        fun createAttributes(): ItemAttributeModifiers =
            ItemAttributeModifiers.builder()
                .add(
                    Attributes.FALL_DAMAGE_MULTIPLIER,
                    AttributeModifier(
                        FALL_REDUCTION_ID,
                        -0.5,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    ),
                    EquipmentSlotGroup.HEAD
                )
                .build()
    }

    // NeoForge extension — returns HEAD slot so it can be equipped via right-click
    override fun getEquipmentSlot(stack: ItemStack): EquipmentSlot = EquipmentSlot.HEAD

    override fun getDefaultAttributeModifiers(stack: ItemStack): ItemAttributeModifiers = createAttributes()
}
