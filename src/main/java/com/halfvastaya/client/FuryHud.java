package com.halfvastaya.client;

import com.halfvastaya.ClientState;
import com.halfvastaya.HVData;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Barra de Furia rosa-morada, dibujada por ENCIMA de la barra de armadura (se adapta a las filas de corazones). */
public class FuryHud implements LayeredDraw.Layer {
    private static final int BAR_W = 81;
    private static final int BAR_H = 7;

    private float shown = 0.0f;

    @Override
    public void render(GuiGraphics gg, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (!ClientState.active || p == null || mc.options.hideGui) {
            return;
        }
        Font font = mc.font;

        // Misma formula que usa vanilla para colocar la fila de armadura
        int w = gg.guiWidth();
        int h = gg.guiHeight();
        int rows = Mth.ceil((p.getMaxHealth() + Mth.ceil(p.getAbsorptionAmount())) / 2.0F / 10.0F);
        int rowH = Math.max(10 - (rows - 2), 3);
        int armorY = h - 39 - (rows - 1) * rowH - 10;

        int x = w / 2 - 91;
        int y = armorY - 12;

        float target = Math.min(ClientState.fury, HVData.READY);
        shown += (target - shown) * 0.25f;
        if (Math.abs(target - shown) < 0.05f) {
            shown = target;
        }
        boolean ready = ClientState.fury >= HVData.READY;
        double t = (System.currentTimeMillis() % 100000L) / 1000.0;
        float pulse = ready ? 0.5f + 0.5f * (float) Math.sin(t * 7.0) : 0.0f;

        // marco
        int border = ready ? lerp(0xFF9D6BFF, 0xFF7DF9FF, pulse) : 0xFF5B2E9E;
        gg.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, border);
        gg.fill(x, y, x + BAR_W, y + BAR_H, 0xFF0E0620);

        // relleno con degradado rosa-morado
        int fw = Math.round(BAR_W * (shown / HVData.READY));
        if (fw > 0) {
            int top = ready ? lerp(0xFFFF9DE0, 0xFF9DF3FF, pulse) : 0xFFFF9DE0;
            gg.fillGradient(x, y, x + fw, y + BAR_H, top, 0xFF7B3FE4);
            // brillo que recorre la barra
            int shine = (int) ((t * 40.0) % (BAR_W + 20)) - 10;
            int sx0 = Math.max(x, x + shine);
            int sx1 = Math.min(x + fw, x + shine + 6);
            if (sx1 > sx0) {
                gg.fill(sx0, y, sx1, y + 2, 0x66FFFFFF);
            }
        }
        // marcas cada 20 %
        for (int i = 1; i < 5; i++) {
            int mx = x + BAR_W * i / 5;
            gg.fill(mx, y, mx + 1, y + BAR_H, 0x88000000);
        }

        // nivel a la izquierda
        String roman = HVData.roman(ClientState.level);
        gg.drawString(font, Component.literal(roman).withStyle(s -> s.withColor(0xFF8AD8).withBold(true)),
                x - font.width(roman) - 4, y, 0xFF8AD8, true);

        // texto a la derecha
        Component label;
        if (ready) {
            int col = lerp(0xFFFF8AD8, 0xFF7DF9FF, pulse);
            label = Component.translatable("halfvastaya.hud.fury").append(" ")
                    .append(Component.translatable("halfvastaya.hud.ready"))
                    .withStyle(s -> s.withColor(col & 0xFFFFFF).withBold(true));
        } else {
            label = Component.translatable("halfvastaya.hud.fury").append(" " + Math.round(shown) + "/" + HVData.READY)
                    .withStyle(s -> s.withColor(0xC9F4FF));
        }
        gg.drawString(font, label, x + BAR_W + 5, y, 0xC9F4FF, true);
    }

    private static int lerp(int a, int b, float t) {
        t = Mth.clamp(t, 0.0f, 1.0f);
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}
