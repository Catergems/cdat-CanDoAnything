package com.gemcaterite.cdat

import com.gemcaterite.cdat.item.AmethystSwordItem
import com.gemcaterite.cdat.item.ChainsawItem
import com.gemcaterite.cdat.item.DrillItem
import com.gemcaterite.cdat.item.HammerItem
import com.mojang.logging.LogUtils
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

@Mod(CanDoAnything.MODID)
class CanDoAnything(modEventBus: IEventBus, modContainer: ModContainer) {

    init {
        ITEMS.register(modEventBus)
        CREATIVE_MODE_TABS.register(modEventBus)
        modEventBus.addListener(::addCreative)
        NeoForge.EVENT_BUS.addListener(::onLivingDeath)
        LOGGER.info("[CanDoAnything] Initialized")
    }

    private fun addCreative(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(DRILL.get())
            event.accept(HAMMER.get())
            event.accept(CHAINSAW.get())
        }
        if (event.tabKey == CreativeModeTabs.COMBAT) {
            event.accept(AMETHYST_SWORD.get())
        }
    }

    private fun onLivingDeath(event: LivingDeathEvent) {
        val dead = event.entity
        if (!AmethystSwordItem.isTargetable(dead)) return
        val serverLevel = dead.level() as? ServerLevel ?: return
        val source = event.source.entity as? net.minecraft.world.entity.LivingEntity ?: return
        val heldItem = source.mainHandItem
        if (heldItem.item !is AmethystSwordItem) return
        AmethystSwordItem.releaseVibration(serverLevel, source, dead.blockPosition(), AmethystSwordItem.KILL_DAMAGE)
    }

    companion object {
        const val MODID = "cdat"
        val LOGGER = LogUtils.getLogger()

        val ITEMS: DeferredRegister.Items = DeferredRegister.createItems(MODID)
        val CREATIVE_MODE_TABS: DeferredRegister<CreativeModeTab> =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID)

        val DRILL: DeferredItem<Item> = ITEMS.registerItem("drill") { props -> DrillItem(props.durability(512)) }
        val HAMMER: DeferredItem<Item> = ITEMS.registerItem("hammer") { props -> HammerItem(props.durability(256)) }
        val AMETHYST_SWORD: DeferredItem<Item> = ITEMS.registerItem("amethyst_sword") { props -> AmethystSwordItem(props.durability(512)) }
        val CHAINSAW: DeferredItem<Item> = ITEMS.registerItem("chainsaw") { props -> ChainsawItem(props.durability(328)) }

        val CDA_TAB: DeferredHolder<CreativeModeTab, CreativeModeTab> =
            CREATIVE_MODE_TABS.register("main", Supplier {
                CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cdat.main"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon { DRILL.get().defaultInstance }
                    .displayItems { _, output ->
                        output.accept(DRILL.get())
                        output.accept(HAMMER.get())
                        output.accept(AMETHYST_SWORD.get())
                        output.accept(CHAINSAW.get())
                    }
                    .build()
            })
    }
}
