package com.halfvastaya.client;

import com.halfvastaya.ClientState;
import com.halfvastaya.HalfVastaya;
import com.halfvastaya.Net;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = HalfVastaya.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (ModKeys.MENU.consumeClick()) {
            if (mc.player != null && mc.screen == null && ClientState.active) {
                mc.setScreen(new HalfVastayaScreen());
            }
        }
        while (ModKeys.FURY.consumeClick()) {
            if (mc.player != null && mc.screen == null && ClientState.active) {
                PacketDistributor.sendToServer(new Net.FuryPayload());
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.reset();
    }
}
