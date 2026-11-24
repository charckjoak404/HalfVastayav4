package com.halfvastaya.client;

import java.util.List;

import com.halfvastaya.ClientState;
import com.halfvastaya.HVData;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/** Panel de progreso: nivel, evolucion, Furia, bonificaciones y controles. Paleta Flor Espiritual: morado, rosa y celeste. */
public class HalfVastayaScreen extends Screen {
    private static final int W = 320;
    private static final int H = 222;

    private static final int ACCENT = 0xFFFF8AD8;
    private static final int LIGHT = 0xFFC9F4FF;
    private static final int DARK = 0xFF6A3DB8;
    private static final int BG_TOP = 0xF02B1257;
    private static final int BG_BOTTOM = 0xF00F0620;

    public HalfVastayaScreen() {
        super(Component.translatable("halfvastaya.screen.title"));
    }

    private int x0() {
        return (this.width - W) / 2;
    }

    private int y0() {
        return Math.max(4, (this.height - H - 28) / 2);
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("halfvastaya.screen.close"), b -> onClose())
                .bounds(this.width / 2 - 40, y0() + H + 6, 80, 20).build());
    }

    @Override
    public void tick() {
        if (!ClientState.active) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeys.MENU.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Se dibuja aqui para quedar DETRAS del boton (Screen.render llama primero a este metodo). */
    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        int x0 = x0();
        int y0 = y0();
        double t = (System.currentTimeMillis() % 1000000L) / 1000.0;

        gg.fill(0, 0, this.width, this.height, 0xB0000000);

        // panel
        gg.fill(x0 - 3, y0 - 3, x0 + W + 3, y0 + H + 3, 0xFF3A1A78);
        gg.fillGradient(x0, y0, x0 + W, y0 + H, BG_TOP, BG_BOTTOM);
        gg.renderOutline(x0, y0, W, H, ACCENT);
        gg.renderOutline(x0 + 3, y0 + 3, W - 6, H - 6, DARK);
        for (int[] c : new int[][]{{x0 - 1, y0 - 1}, {x0 + W - 2, y0 - 1}, {x0 - 1, y0 + H - 2}, {x0 + W - 2, y0 + H - 2}}) {
            gg.fill(c[0], c[1], c[0] + 3, c[1] + 3, LIGHT);
        }

        sparkles(gg, x0, y0, t);

        // titulo
        gg.pose().pushPose();
        gg.pose().translate(this.width / 2.0f, y0 + 9, 0);
        gg.pose().scale(1.5f, 1.5f, 1.0f);
        gg.drawCenteredString(this.font, this.title.copy().withStyle(ChatFormatting.BOLD), 0, 0, ACCENT);
        gg.pose().popPose();

        if (!ClientState.active) {
            List<FormattedCharSequence> lines = this.font.split(Component.translatable("halfvastaya.screen.inactive"), W - 50);
            int ly = y0 + 60;
            for (FormattedCharSequence l : lines) {
                gg.drawString(this.font, l, x0 + 25, ly, LIGHT, true);
                ly += 11;
            }
            drawControls(gg, x0, y0 + 150);
            return;
        }

        int level = ClientState.level;
        int dmg = ClientState.damage;

        // subtitulo de nivel
        gg.drawCenteredString(this.font,
                Component.translatable("halfvastaya.screen.level", HVData.roman(level)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                this.width / 2, y0 + 27, LIGHT);

        drawTrack(gg, x0, y0 + 42, level, dmg, t);
        drawEvolution(gg, x0, y0 + 76, level, dmg, t);
        drawFury(gg, x0, y0 + 112, t);
        drawBonuses(gg, x0, y0 + 148, level);
        drawControls(gg, x0, y0 + 168);
    }

    // ------------------------------------------------------------------ secciones

    private void drawTrack(GuiGraphics gg, int x0, int y, int level, int dmg, double t) {
        int[] cx = {x0 + 55, x0 + 160, x0 + 265};
        float seg1 = level >= 2 ? 1.0f : Mth.clamp(dmg / (float) HVData.LEVEL2_DAMAGE, 0.0f, 1.0f);
        float seg2 = level >= 3 ? 1.0f : level == 2
                ? Mth.clamp((dmg - HVData.LEVEL2_DAMAGE) / (float) (HVData.LEVEL3_DAMAGE - HVData.LEVEL2_DAMAGE), 0.0f, 1.0f) : 0.0f;
        line(gg, cx[0] + 11, cx[1] - 11, y + 8, seg1);
        line(gg, cx[1] + 11, cx[2] - 11, y + 8, seg2);

        int[] thresholds = {0, HVData.LEVEL2_DAMAGE, HVData.LEVEL3_DAMAGE};
        for (int n = 1; n <= 3; n++) {
            int c = cx[n - 1];
            boolean reached = level >= n;
            boolean current = level == n;
            float pulse = current ? 0.5f + 0.5f * (float) Math.sin(t * 5.0) : 0.0f;
            int border = current ? lerp(ACCENT, 0xFFFFFFFF, pulse * 0.8f) : reached ? ACCENT : DARK;
            gg.fill(c - 11, y - 3, c + 11, y + 19, border);
            gg.fill(c - 10, y - 2, c + 10, y + 18, reached ? 0xFF7B3FE4 : 0xFF150A2B);
            if (reached) {
                gg.fillGradient(c - 10, y - 2, c + 10, y + 18, 0xFFFF9DE0, 0xFF8A4DFF);
            }
            gg.drawCenteredString(this.font, HVData.roman(n), c, y + 4, reached ? 0xFF1B0A33 : DARK);
            gg.drawCenteredString(this.font, thresholds[n - 1] + "", c, y + 22, reached ? LIGHT : DARK);
        }
    }

    private void line(GuiGraphics gg, int xa, int xb, int y, float progress) {
        gg.fill(xa, y - 1, xb, y + 2, 0xFF1E1038);
        int fx = xa + Math.round((xb - xa) * progress);
        if (fx > xa) {
            gg.fillGradient(xa, y - 1, fx, y + 2, 0xFFFF9DE0, 0xFF8A4DFF);
        }
    }

    private void drawEvolution(GuiGraphics gg, int x0, int y, int level, int dmg, double t) {
        gg.drawString(this.font, Component.translatable("halfvastaya.screen.progress").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                x0 + 10, y, ACCENT, true);

        float frac;
        Component value;
        Component note;
        if (level >= 3) {
            frac = 1.0f;
            value = Component.literal(HVData.LEVEL3_DAMAGE + " / " + HVData.LEVEL3_DAMAGE);
            note = Component.translatable("halfvastaya.screen.progress.max");
        } else {
            int base = level == 1 ? 0 : HVData.LEVEL2_DAMAGE;
            int goal = level == 1 ? HVData.LEVEL2_DAMAGE : HVData.LEVEL3_DAMAGE;
            frac = Mth.clamp((dmg - base) / (float) (goal - base), 0.0f, 1.0f);
            value = Component.translatable("halfvastaya.screen.progress.value", dmg, goal);
            note = Component.translatable("halfvastaya.screen.progress.left", Math.max(0, goal - dmg));
        }
        gg.drawString(this.font, value.copy().withStyle(ChatFormatting.AQUA), x0 + W - 10 - this.font.width(value), y, LIGHT, true);
        bar(gg, x0 + 10, y + 11, W - 20, 8, frac, false, t);
        gg.drawString(this.font, note.copy().withStyle(ChatFormatting.GRAY), x0 + 10, y + 22, 0xFFAAAAAA, true);
    }

    private void drawFury(GuiGraphics gg, int x0, int y, double t) {
        boolean ready = ClientState.fury >= HVData.READY;
        gg.drawString(this.font, Component.translatable("halfvastaya.screen.fury").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                x0 + 10, y, ACCENT, true);
        Component value = Component.literal(Math.min(ClientState.fury, HVData.READY) + " / " + HVData.READY);
        gg.drawString(this.font, value.copy().withStyle(ChatFormatting.AQUA), x0 + W - 10 - this.font.width(value), y, LIGHT, true);
        bar(gg, x0 + 10, y + 11, W - 20, 8, Mth.clamp(ClientState.fury / (float) HVData.READY, 0.0f, 1.0f), ready, t);
        Component note = Component.translatable(ready ? "halfvastaya.screen.fury.ready" : "halfvastaya.screen.fury.charging");
        gg.drawString(this.font, note.copy().withStyle(ready ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY),
                x0 + 10, y + 22, ready ? ACCENT : 0xFFAAAAAA, true);
    }

    private void drawBonuses(GuiGraphics gg, int x0, int y, int level) {
        gg.drawString(this.font, Component.translatable("halfvastaya.screen.bonuses").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                x0 + 10, y, ACCENT, true);
        gg.drawString(this.font, Component.translatable("halfvastaya.screen.bonus.attack", fmt(HVData.attackBonus(level))).withStyle(ChatFormatting.AQUA),
                x0 + 150, y, LIGHT, true);
        gg.drawString(this.font, Component.translatable("halfvastaya.screen.bonus.armor", fmt(HVData.armorBonus(level))).withStyle(ChatFormatting.AQUA),
                x0 + 150, y + 11, LIGHT, true);
    }

    private void drawControls(GuiGraphics gg, int x0, int y) {
        gg.fill(x0 + 10, y - 4, x0 + W - 10, y - 3, DARK);
        gg.drawString(this.font, Component.translatable("halfvastaya.screen.controls").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                x0 + 10, y, ACCENT, true);
        gg.drawString(this.font, Component.translatable("halfvastaya.screen.control.fury", ModKeys.FURY.getTranslatedKeyMessage()).withStyle(ChatFormatting.AQUA),
                x0 + 10, y + 12, LIGHT, true);
        gg.drawString(this.font, Component.translatable("halfvastaya.screen.control.menu", ModKeys.MENU.getTranslatedKeyMessage()).withStyle(ChatFormatting.AQUA),
                x0 + 10, y + 23, LIGHT, true);
        gg.drawString(this.font, Component.translatable("halfvastaya.screen.control.hint").withStyle(ChatFormatting.GRAY),
                x0 + 10, y + 36, 0xFFAAAAAA, true);
    }

    // ------------------------------------------------------------------ utilidades de dibujo

    private void bar(GuiGraphics gg, int x, int y, int w, int h, float frac, boolean glow, double t) {
        float pulse = glow ? 0.5f + 0.5f * (float) Math.sin(t * 7.0) : 0.0f;
        gg.fill(x - 1, y - 1, x + w + 1, y + h + 1, glow ? lerp(0xFF9D6BFF, 0xFF7DF9FF, pulse) : DARK);
        gg.fill(x, y, x + w, y + h, 0xFF0E0620);
        int fw = Math.round(w * frac);
        if (fw > 0) {
            gg.fillGradient(x, y, x + fw, y + h, glow ? lerp(0xFFC9F4FF, 0xFFFFFFFF, pulse * 0.6f) : 0xFFFF9DE0, 0xFF8A4DFF);
            int shine = (int) ((t * 45.0) % (w + 24)) - 12;
            int s0 = Math.max(x, x + shine);
            int s1 = Math.min(x + fw, x + shine + 7);
            if (s1 > s0) {
                gg.fill(s0, y, s1, y + 2, 0x66FFFFFF);
            }
        }
        for (int i = 1; i < 10; i++) {
            int mx = x + w * i / 10;
            gg.fill(mx, y, mx + 1, y + h, 0x66000000);
        }
    }

    private void sparkles(GuiGraphics gg, int x0, int y0, double t) {
        for (int i = 0; i < 22; i++) {
            int sx = x0 + 6 + (i * 53) % (W - 12);
            double speed = 8.0 + (i % 5) * 3.0;
            int sy = y0 + H - 4 - (int) ((t * speed + i * 37) % (H - 8));
            int alpha = 40 + (int) (50 * (0.5 + 0.5 * Math.sin(t * 2.0 + i)));
            int sc = i % 3 == 0 ? 0x7DF9FF : i % 3 == 1 ? 0xFF8AD8 : 0xC9A0FF;
            gg.fill(sx, sy, sx + 1, sy + 1, (alpha << 24) | sc);
        }
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    private static int lerp(int a, int b, float t) {
        t = Mth.clamp(t, 0.0f, 1.0f);
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}
