package com.halfvastaya.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ModKeys {
    public static final String CATEGORY = "key.categories.halfvastaya";

    /** Golpe Verdadero (por defecto R). */
    public static final KeyMapping FURY = new KeyMapping("key.halfvastaya.fury",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    /** Panel de progreso (por defecto G). */
    public static final KeyMapping MENU = new KeyMapping("key.halfvastaya.menu",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    private ModKeys() {
    }
}
