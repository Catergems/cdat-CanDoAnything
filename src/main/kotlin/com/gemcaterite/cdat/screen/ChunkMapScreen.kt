package com.gemcaterite.cdat.screen

import com.daqem.uilib.gui.AbstractScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.levelgen.Heightmap

class ChunkMapScreen(private val centerChunk: ChunkPos) : AbstractScreen(Component.translatable("screen.cdat.chunk_map")) {

    companion object {
        private const val RADIUS = 5
        private const val CELL_SIZE = 18
        private const val GRID_SIZE = (RADIUS * 2 + 1) * CELL_SIZE

        private const val COLOR_OCEAN    = 0xFF1A6B8A.toInt()
        private const val COLOR_PLAINS   = 0xFF89B65A.toInt()
        private const val COLOR_FOREST   = 0xFF2D6B3A.toInt()
        private const val COLOR_DESERT   = 0xFFD4B24A.toInt()
        private const val COLOR_MOUNTAIN = 0xFF888888.toInt()
        private const val COLOR_SNOW     = 0xFFEEEEFF.toInt()
        private const val COLOR_SWAMP    = 0xFF4A5E3A.toInt()
        private const val COLOR_UNKNOWN  = 0xFF555555.toInt()
        private const val COLOR_BORDER   = 0xFF222222.toInt()
        private const val COLOR_CENTER   = 0xFFFFD700.toInt()
        private const val COLOR_BG       = 0xFF1A1A1A.toInt()
        private const val COLOR_PANEL    = 0xFF2A2A2A.toInt()
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)

        val level = Minecraft.getInstance().level ?: return

        val panelX = (width - GRID_SIZE - 20) / 2
        val panelY = (height - GRID_SIZE - 36) / 2
        val panelW = GRID_SIZE + 20
        val panelH = GRID_SIZE + 36

        guiGraphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL)
        guiGraphics.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, COLOR_BG)

        guiGraphics.drawCenteredString(font, Component.translatable("screen.cdat.chunk_map"), width / 2, panelY + 6, 0xFFFFFF)

        val gridStartX = panelX + 10
        val gridStartY = panelY + 20

        for (dz in -RADIUS..RADIUS) {
            for (dx in -RADIUS..RADIUS) {
                val chunkX = centerChunk.x + dx
                val chunkZ = centerChunk.z + dz
                val cellX = gridStartX + (dx + RADIUS) * CELL_SIZE
                val cellZ = gridStartY + (dz + RADIUS) * CELL_SIZE

                val color = getChunkColor(level, ChunkPos(chunkX, chunkZ))
                guiGraphics.fill(cellX, cellZ, cellX + CELL_SIZE - 1, cellZ + CELL_SIZE - 1, color)

                if (dx == 0 && dz == 0) {
                    guiGraphics.fill(cellX, cellZ, cellX + CELL_SIZE - 1, cellZ + 1, COLOR_CENTER)
                    guiGraphics.fill(cellX, cellZ + CELL_SIZE - 2, cellX + CELL_SIZE - 1, cellZ + CELL_SIZE - 1, COLOR_CENTER)
                    guiGraphics.fill(cellX, cellZ, cellX + 1, cellZ + CELL_SIZE - 1, COLOR_CENTER)
                    guiGraphics.fill(cellX + CELL_SIZE - 2, cellZ, cellX + CELL_SIZE - 1, cellZ + CELL_SIZE - 1, COLOR_CENTER)
                }

                guiGraphics.fill(cellX + CELL_SIZE - 1, cellZ, cellX + CELL_SIZE, cellZ + CELL_SIZE, COLOR_BORDER)
                guiGraphics.fill(cellX, cellZ + CELL_SIZE - 1, cellX + CELL_SIZE, cellZ + CELL_SIZE, COLOR_BORDER)
            }
        }

        guiGraphics.drawCenteredString(font, "Chunk: ${centerChunk.x}, ${centerChunk.z}", width / 2, panelY + panelH - 12, 0xAAAAAA)
    }

    private fun getChunkColor(level: Level, chunkPos: ChunkPos): Int {
        level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z) ?: return COLOR_UNKNOWN

        val cx = chunkPos.minBlockX + 8
        val cz = chunkPos.minBlockZ + 8
        val surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz)
        val biome = level.getBiome(BlockPos(cx, surfaceY, cz))

        return when {
            biome.`is`(Biomes.OCEAN) || biome.`is`(Biomes.DEEP_OCEAN) ||
            biome.`is`(Biomes.COLD_OCEAN) || biome.`is`(Biomes.WARM_OCEAN) ||
            biome.`is`(Biomes.FROZEN_OCEAN) || biome.`is`(Biomes.DEEP_COLD_OCEAN) ||
            biome.`is`(Biomes.DEEP_FROZEN_OCEAN) || biome.`is`(Biomes.DEEP_LUKEWARM_OCEAN) ||
            biome.`is`(Biomes.LUKEWARM_OCEAN) || biome.`is`(Biomes.RIVER) ||
            biome.`is`(Biomes.FROZEN_RIVER) -> COLOR_OCEAN

            biome.`is`(Biomes.DESERT) || biome.`is`(Biomes.BADLANDS) ||
            biome.`is`(Biomes.ERODED_BADLANDS) || biome.`is`(Biomes.WOODED_BADLANDS) -> COLOR_DESERT

            biome.`is`(Biomes.SNOWY_PLAINS) || biome.`is`(Biomes.FROZEN_PEAKS) ||
            biome.`is`(Biomes.SNOWY_SLOPES) || biome.`is`(Biomes.ICE_SPIKES) ||
            biome.`is`(Biomes.SNOWY_BEACH) || biome.`is`(Biomes.SNOWY_TAIGA) -> COLOR_SNOW

            biome.`is`(Biomes.SWAMP) || biome.`is`(Biomes.MANGROVE_SWAMP) -> COLOR_SWAMP

            biome.`is`(Biomes.FOREST) || biome.`is`(Biomes.BIRCH_FOREST) ||
            biome.`is`(Biomes.DARK_FOREST) || biome.`is`(Biomes.FLOWER_FOREST) ||
            biome.`is`(Biomes.OLD_GROWTH_BIRCH_FOREST) || biome.`is`(Biomes.TAIGA) ||
            biome.`is`(Biomes.OLD_GROWTH_PINE_TAIGA) || biome.`is`(Biomes.OLD_GROWTH_SPRUCE_TAIGA) ||
            biome.`is`(Biomes.JUNGLE) || biome.`is`(Biomes.SPARSE_JUNGLE) ||
            biome.`is`(Biomes.BAMBOO_JUNGLE) -> COLOR_FOREST

            biome.`is`(Biomes.JAGGED_PEAKS) || biome.`is`(Biomes.STONY_PEAKS) ||
            biome.`is`(Biomes.STONY_SHORE) || biome.`is`(Biomes.WINDSWEPT_HILLS) ||
            biome.`is`(Biomes.WINDSWEPT_GRAVELLY_HILLS) || biome.`is`(Biomes.WINDSWEPT_FOREST) ||
            biome.`is`(Biomes.WINDSWEPT_SAVANNA) -> COLOR_MOUNTAIN

            else -> COLOR_PLAINS
        }
    }

    override fun isPauseScreen(): Boolean = false
}
