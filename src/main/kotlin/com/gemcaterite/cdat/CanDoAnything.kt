package com.gemcaterite.cdat

import com.gemcaterite.cdat.block.ChunkLoaderBlock
import com.gemcaterite.cdat.block.ChunkLoaderBlockEntity
import com.gemcaterite.cdat.item.AmethystSwordItem
import com.gemcaterite.cdat.item.DrillItem
import com.gemcaterite.cdat.item.HammerItem
import com.gemcaterite.cdat.network.ChunkForcePacket
import com.mojang.logging.LogUtils
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

@Mod(CanDoAnything.MODID)
class CanDoAnything(modEventBus: IEventBus, modContainer: ModContainer) {

    init {
        ITEMS.register(modEventBus)
        BLOCKS.register(modEventBus)
        BLOCK_ENTITY_TYPES.register(modEventBus)
        CREATIVE_MODE_TABS.register(modEventBus)
        modEventBus.addListener(::addCreative)
        modEventBus.addListener(ChunkForcePacket::register)
        NeoForge.EVENT_BUS.addListener(::onLivingDeath)
        LOGGER.info("[CanDoAnything] Initialized")
    }

    private fun addCreative(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(DRILL.get())
            event.accept(HAMMER.get())
            event.accept(CHUNK_LOADER_ITEM.get())
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
        val BLOCKS: DeferredRegister.Blocks = DeferredRegister.createBlocks(MODID)
        val BLOCK_ENTITY_TYPES: DeferredRegister<BlockEntityType<*>> =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID)
        val CREATIVE_MODE_TABS: DeferredRegister<CreativeModeTab> =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID)

        val DRILL: DeferredItem<Item> = ITEMS.registerItem("drill") { props -> DrillItem(props.durability(512)) }
        val HAMMER: DeferredItem<Item> = ITEMS.registerItem("hammer") { props -> HammerItem(props.durability(256)) }
        val AMETHYST_SWORD: DeferredItem<Item> = ITEMS.registerItem("amethyst_sword") { props -> AmethystSwordItem(props.durability(512)) }

        val CHUNK_LOADER_BLOCK: DeferredBlock<Block> = BLOCKS.registerBlock("chunk_loader") { props -> ChunkLoaderBlock(props) }
        val CHUNK_LOADER_ITEM: DeferredItem<Item> = ITEMS.registerItem("chunk_loader") { props -> BlockItem(CHUNK_LOADER_BLOCK.get(), props) }
        val CHUNK_LOADER_BE_TYPE: DeferredHolder<BlockEntityType<*>, BlockEntityType<ChunkLoaderBlockEntity>> =
            BLOCK_ENTITY_TYPES.register("chunk_loader", Supplier {
                BlockEntityType({ pos, state -> ChunkLoaderBlockEntity(pos, state) }, setOf(CHUNK_LOADER_BLOCK.get()))
            })

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
                        output.accept(CHUNK_LOADER_ITEM.get())
                    }
                    .build()
            })
    }
}
