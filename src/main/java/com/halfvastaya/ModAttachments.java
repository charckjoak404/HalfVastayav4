package com.halfvastaya;

import java.util.function.Supplier;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HalfVastaya.MODID);

    public static final Supplier<AttachmentType<HVData>> DATA = ATTACHMENTS.register("data",
            () -> AttachmentType.builder(() -> new HVData())
                    .serialize(HVData.CODEC)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {
    }
}
