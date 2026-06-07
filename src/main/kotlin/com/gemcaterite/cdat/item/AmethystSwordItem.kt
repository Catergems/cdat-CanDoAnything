package com.gemcaterite.cdat.item

import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.AABB

class AmethystSwordItem(properties: Properties) : Item(properties) {

    companion object {
        const val VIBRATION_RADIUS = 10.0
        const val HIT_DAMAGE = 2.0
        const val KILL_DAMAGE = 5.0

        fun createAttributes(): ItemAttributeModifiers =
            ItemAttributeModifiers.builder()
                .add(
                    Attributes.ATTACK_DAMAGE,
                    AttributeModifier(
                        Identifier.fromNamespaceAndPath("cdat", "amethyst_sword_damage"),
                        4.0,
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .add(
                    Attributes.ATTACK_SPEED,
                    AttributeModifier(
                        Identifier.fromNamespaceAndPath("cdat", "amethyst_sword_speed"),
                        -2.4,
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .build()

        fun isHostile(entity: Entity): Boolean = entity is Enemy

        fun isPassive(entity: Entity): Boolean = entity is Animal

        fun isTargetable(entity: Entity): Boolean = isHostile(entity) || isPassive(entity)

        fun releaseVibration(
            serverLevel: ServerLevel,
            attacker: LivingEntity,
            epicenter: BlockPos,
            damageHostile: Double   // passive animals are NEVER damaged, only glowed
        ) {
            serverLevel.gameEvent(GameEvent.SCULK_SENSOR_TENDRILS_CLICKING, epicenter, GameEvent.Context.of(attacker))

            val box = AABB(
                epicenter.x - VIBRATION_RADIUS, epicenter.y - VIBRATION_RADIUS, epicenter.z - VIBRATION_RADIUS,
                epicenter.x + VIBRATION_RADIUS, epicenter.y + VIBRATION_RADIUS, epicenter.z + VIBRATION_RADIUS
            )
            val nearby = serverLevel.getEntitiesOfClass(LivingEntity::class.java, box) { e ->
                isTargetable(e) && e !== attacker && e.isAlive
            }

            for (entity in nearby) {
                // Always glow
                entity.addEffect(MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false))

                // Only damage hostile mobs, never passive animals
                if (isHostile(entity)) {
                    entity.hurt(serverLevel.damageSources().magic(), damageHostile.toFloat())
                }
            }
        }
    }

    override fun hurtEnemy(stack: ItemStack, target: LivingEntity, attacker: LivingEntity) {
        if (!isTargetable(target)) return
        val serverLevel = attacker.level() as? ServerLevel ?: return

        if (target.isAlive) {
            releaseVibration(serverLevel, attacker, target.blockPosition(), HIT_DAMAGE)
        }

        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND)
    }

    override fun getDefaultAttributeModifiers(stack: ItemStack): ItemAttributeModifiers = createAttributes()
}
