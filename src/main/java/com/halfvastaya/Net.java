package com.halfvastaya;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Net {

    /** Servidor -> cliente: estado actual del jugador. */
    public record SyncPayload(boolean active, int level, int damage, int fury) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncPayload> TYPE =
                new CustomPacketPayload.Type<>(HalfVastaya.id("sync"));
        public static final StreamCodec<ByteBuf, SyncPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, SyncPayload::active,
                ByteBufCodecs.VAR_INT, SyncPayload::level,
                ByteBufCodecs.VAR_INT, SyncPayload::damage,
                ByteBufCodecs.VAR_INT, SyncPayload::fury,
                SyncPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Cliente -> servidor: el jugador pulso la tecla del Golpe Verdadero. */
    public record FuryPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<FuryPayload> TYPE =
                new CustomPacketPayload.Type<>(HalfVastaya.id("fury"));
        public static final StreamCodec<ByteBuf, FuryPayload> CODEC = StreamCodec.unit(new FuryPayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SyncPayload.TYPE, SyncPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        ClientState.apply(payload.active(), payload.level(), payload.damage(), payload.fury())));
        registrar.playToServer(FuryPayload.TYPE, FuryPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        HVLogic.tryFury(sp);
                    }
                }));
    }

    private Net() {
    }
}
