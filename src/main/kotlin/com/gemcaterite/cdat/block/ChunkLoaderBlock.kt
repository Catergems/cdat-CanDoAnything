package com.gemcaterite.cdat.block

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class ChunkLoaderBlock(settings: Properties) : BaseEntityBlock(settings) {

    companion object {
        val CODEC: MapCodec<ChunkLoaderBlock> = simpleCodec(::ChunkLoaderBlock)

        fun setChunksForced(level: ServerLevel, pos: BlockPos, force: Boolean) {
            val chunkPos = ChunkPos(pos)
            for (dx in -1..1) {
                for (dz in -1..1) {
                    level.setChunkForced(chunkPos.x + dx, chunkPos.z + dz, force)
                }
            }
        }
    }

    override fun codec(): MapCodec<out BaseEntityBlock> = CODEC

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        ChunkLoaderBlockEntity(pos, state)

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? = null

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) {
            val chunkPos = ChunkPos(pos)
            net.minecraft.client.Minecraft.getInstance().setScreen(
                com.gemcaterite.cdat.screen.ChunkMapScreen(chunkPos)
            )
        }
        return InteractionResult.SUCCESS
    }

    override fun onPlace(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        oldState: BlockState,
        movedByPiston: Boolean
    ) {
        if (level is ServerLevel && oldState.block != this) {
            setChunksForced(level, pos, true)
        }
    }

    override fun playerWillDestroy(
        level: Level,
        pos: BlockPos,
        state: BlockState,
        player: Player
    ): BlockState {
        if (level is ServerLevel) {
            setChunksForced(level, pos, false)
        }
        return super.playerWillDestroy(level, pos, state, player)
    }
}
