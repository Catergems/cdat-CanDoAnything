package com.gemcaterite.cdat.screen

import com.daqem.uilib.gui.AbstractScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.material.MapColor

class ChunkMapScreen(
    private val centerChunk: ChunkPos,
    private val forcedChunks: MutableSet<ChunkPos> = mutableSetOf()
) : AbstractScreen(Component.translatable("screen.cdat.chunk_map")) {

    companion object {
        private const val RADIUS = 5
        private const val CELL_SIZE = 20
        private const val SAMPLES = 4      // sample 4x4 blocks per chunk for color
        private const val GRID_SIZE = (RADIUS * 2 + 1) * CELL_SIZE

        private const val COLOR_UNKNOWN  = 0xFF555555.toInt()
        private const val COLOR_BORDER   = 0xFF111111.toInt()
        private const val COLOR_BG       = 0xFF1A1A1A.toInt()
        private const val COLOR_PANEL    = 0xFF2A2A2A.toInt()
        private const val COLOR_FORCED   = 0xFF00FF44.toInt()  // green = force-loaded
        private const val COLOR_SELECTED = 0xFFFFD700.toInt()  // gold = center/selected
        private const val COLOR_PLAYER   = 0xFFFF4444.toInt()  // red = player
        private const val COLOR_HOVER    = 0x44FFFFFF.toInt()  // white tint on hover
    }

    // Cache chunk colors so we don't re-sample every frame
    private val colorCache = mutableMapOf<ChunkPos, Int>()

    private var hoveredChunk: ChunkPos? = null

    private fun getGridStart(): Pair<Int, Int> {
        val panelX = (width - GRID_SIZE - 20) / 2
        val panelY = (height - GRID_SIZE - 40) / 2
        return panelX + 10 to panelY + 24
    }

    private fun chunkAtPixel(mouseX: Int, mouseY: Int): ChunkPos? {
        val (gx, gy) = getGridStart()
        val dx = mouseX - gx
        val dz = mouseY - gy
        if (dx < 0 || dz < 0 || dx >= GRID_SIZE || dz >= GRID_SIZE) return null
        val cx = centerChunk.x + (dx / CELL_SIZE) - RADIUS
        val cz = centerChunk.z + (dz / CELL_SIZE) - RADIUS
        return ChunkPos(cx, cz)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)

        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val player = mc.player ?: return

        hoveredChunk = chunkAtPixel(mouseX, mouseY)

        val panelX = (width - GRID_SIZE - 20) / 2
        val panelY = (height - GRID_SIZE - 40) / 2
        val panelW = GRID_SIZE + 20
        val panelH = GRID_SIZE + 40

        // Panel
        guiGraphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL)
        guiGraphics.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, COLOR_BG)

        // Title
        guiGraphics.drawCenteredString(font, Component.translatable("screen.cdat.chunk_map"), width / 2, panelY + 6, 0xFFFFFF)

        // Legend
        val legendY = panelY + panelH - 22
        guiGraphics.fill(panelX + 8, legendY + 3, panelX + 16, legendY + 11, COLOR_FORCED)
        guiGraphics.drawString(font, "Loaded", panelX + 18, legendY + 3, 0xAAAAAA, false)
        guiGraphics.fill(panelX + 65, legendY + 3, panelX + 73, legendY + 11, COLOR_SELECTED)
        guiGraphics.drawString(font, "Center", panelX + 75, legendY + 3, 0xAAAAAA, false)
        guiGraphics.fill(panelX + 122, legendY + 3, panelX + 130, legendY + 11, COLOR_PLAYER)
        guiGraphics.drawString(font, "You", panelX + 132, legendY + 3, 0xAAAAAA, false)

        // Hint
        guiGraphics.drawCenteredString(font, "Click chunk to toggle force-load", width / 2, panelY + panelH - 10, 0x888888)

        val (gridStartX, gridStartY) = getGridStart()
        val playerChunk = ChunkPos(player.blockPosition())

        for (dz in -RADIUS..RADIUS) {
            for (dx in -RADIUS..RADIUS) {
                val chunkPos = ChunkPos(centerChunk.x + dx, centerChunk.z + dz)
                val cellX = gridStartX + (dx + RADIUS) * CELL_SIZE
                val cellZ = gridStartY + (dz + RADIUS) * CELL_SIZE

                // Terrain color
                val terrainColor = colorCache.getOrPut(chunkPos) { sampleChunkColor(level, chunkPos) }
                guiGraphics.fill(cellX, cellZ, cellX + CELL_SIZE - 1, cellZ + CELL_SIZE - 1, terrainColor)

                // Hover overlay
                if (hoveredChunk == chunkPos) {
                    guiGraphics.fill(cellX, cellZ, cellX + CELL_SIZE - 1, cellZ + CELL_SIZE - 1, COLOR_HOVER)
                }

                // Border color by state
                val borderColor = when {
                    chunkPos == ChunkPos(centerChunk.x, centerChunk.z) -> COLOR_SELECTED
                    forcedChunks.contains(chunkPos) -> COLOR_FORCED
                    chunkPos == playerChunk -> COLOR_PLAYER
                    else -> COLOR_BORDER
                }

                // Draw 1px border
                guiGraphics.fill(cellX, cellZ, cellX + CELL_SIZE - 1, cellZ + 1, borderColor)
                guiGraphics.fill(cellX, cellZ + CELL_SIZE - 2, cellX + CELL_SIZE - 1, cellZ + CELL_SIZE - 1, borderColor)
                guiGraphics.fill(cellX, cellZ, cellX + 1, cellZ + CELL_SIZE - 1, borderColor)
                guiGraphics.fill(cellX + CELL_SIZE - 2, cellZ, cellX + CELL_SIZE - 1, cellZ + CELL_SIZE - 1, borderColor)
            }
        }

        // Player dot — precise position within their chunk cell
        val playerDx = centerChunk.x - playerChunk.x
        val playerDz = centerChunk.z - playerChunk.z
        if (playerDx in -RADIUS..RADIUS && playerDz in -RADIUS..RADIUS) {
            val pCellX = gridStartX + (playerDx.coerceIn(-RADIUS, RADIUS) + RADIUS) * CELL_SIZE
            val pCellZ = gridStartY + (playerDz.coerceIn(-RADIUS, RADIUS) + RADIUS) * CELL_SIZE
            // Sub-chunk offset within the cell
            val subX = ((player.x - playerChunk.minBlockX) / 16.0 * CELL_SIZE).toInt().coerceIn(2, CELL_SIZE - 4)
            val subZ = ((player.z - playerChunk.minBlockZ) / 16.0 * CELL_SIZE).toInt().coerceIn(2, CELL_SIZE - 4)
            guiGraphics.fill(pCellX + subX - 1, pCellZ + subZ - 1, pCellX + subX + 2, pCellZ + subZ + 2, COLOR_PLAYER)
        }

        // Tooltip on hover
        hoveredChunk?.let { hc ->
            val isForced = forcedChunks.contains(hc)
            val tooltip = "Chunk ${hc.x}, ${hc.z}  |  ${if (isForced) "§aForce-loaded§r" else "§7Click to load§r"}"
            guiGraphics.drawString(font, tooltip, panelX + 8, panelY + 14, 0xCCCCCC, false)
        }
    }

    override fun mouseClicked(event: net.minecraft.client.input.MouseButtonEvent, bl: Boolean): Boolean {
        if (event.button() == 0) {
            val clicked = chunkAtPixel(event.x().toInt(), event.y().toInt())
            if (clicked != null) {
                if (forcedChunks.contains(clicked)) {
                    forcedChunks.remove(clicked)
                } else {
                    forcedChunks.add(clicked)
                }
                com.gemcaterite.cdat.network.ChunkForcePacket.send(clicked, forcedChunks.contains(clicked))
                colorCache.remove(clicked)
                return true
            }
        }
        return super.mouseClicked(event, bl)
    }

    private fun sampleChunkColor(level: Level, chunkPos: ChunkPos): Int {
        if (level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z) == null) return COLOR_UNKNOWN

        var r = 0; var g = 0; var b = 0; var count = 0

        val step = 16 / SAMPLES
        for (sx in 0 until SAMPLES) {
            for (sz in 0 until SAMPLES) {
                val bx = chunkPos.minBlockX + sx * step + step / 2
                val bz = chunkPos.minBlockZ + sz * step + step / 2
                val by = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz) - 1
                val state = level.getBlockState(BlockPos(bx, by, bz))
                val mapColor = state.getMapColor(level, BlockPos(bx, by, bz))
                if (mapColor != MapColor.NONE) {
                    val argb = mapColor.calculateARGBColor(MapColor.Brightness.NORMAL)
                    r += (argb shr 16) and 0xFF
                    g += (argb shr 8) and 0xFF
                    b += argb and 0xFF
                    count++
                }
            }
        }

        return if (count == 0) COLOR_UNKNOWN
        else (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
    }

    override fun isPauseScreen(): Boolean = false
}
