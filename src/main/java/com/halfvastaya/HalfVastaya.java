package com.halfvastaya;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(HalfVastaya.MODID)
public class HalfVastaya {
    public static final String MODID = "halfvastaya";

    public HalfVastaya(IEventBus modBus) {
        ModAttachments.ATTACHMENTS.register(modBus);
        modBus.addListener(Net::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
