package com.gemcaterite.cdat.block

import com.gemcaterite.cdat.CanDoAnything
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class ChunkLoaderBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(CanDoAnything.CHUNK_LOADER_BE_TYPE.get(), pos, state)
