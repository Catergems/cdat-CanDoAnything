package com.gemcaterite.cdat.network

import com.gemcaterite.cdat.CanDoAnything
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos
import net.neoforged.neoforge.client.network.ClientPacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.network.registration.PayloadRegistrar

data class ChunkForcePacket(val chunkX: Int, val chunkZ: Int, val force: Boolean) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<ChunkForcePacket> = TYPE

    companion object {
        val ID = Identifier.fromNamespaceAndPath(CanDoAnything.MODID, "chunk_force")
        val TYPE = CustomPacketPayload.Type<ChunkForcePacket>(ID)

        val STREAM_CODEC: StreamCodec<ByteBuf, ChunkForcePacket> = StreamCodec.of(
            { buf, packet ->
                buf.writeInt(packet.chunkX)
                buf.writeInt(packet.chunkZ)
                buf.writeBoolean(packet.force)
            },
            { buf -> ChunkForcePacket(buf.readInt(), buf.readInt(), buf.readBoolean()) }
        )

        fun send(chunkPos: ChunkPos, force: Boolean) {
            ClientPacketDistributor.sendToServer(ChunkForcePacket(chunkPos.x, chunkPos.z, force))
        }

        fun handle(packet: ChunkForcePacket, context: IPayloadContext) {
            context.enqueueWork {
                val player = context.player() as? ServerPlayer ?: return@enqueueWork
                val serverLevel = player.level() as? ServerLevel ?: return@enqueueWork
                serverLevel.setChunkForced(packet.chunkX, packet.chunkZ, packet.force)
            }
        }

        fun register(event: RegisterPayloadHandlersEvent) {
            val registrar: PayloadRegistrar = event.registrar("1")
            registrar.playToServer(TYPE, STREAM_CODEC, ::handle)
        }
    }
}
