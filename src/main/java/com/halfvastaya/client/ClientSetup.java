package com.halfvastaya.client;

import com.halfvastaya.HalfVastaya;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = HalfVastaya.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeys.FURY);
        event.register(ModKeys.MENU);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        // Se dibuja justo despues de la barra de armadura, es decir, POR ENCIMA de ella.
        event.registerAbove(VanillaGuiLayers.ARMOR_LEVEL, HalfVastaya.id("fury_bar"), new FuryHud());
    }
}
