package com.halfvastaya;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Datos del jugador (se guardan y sobreviven a la muerte). Sin clases de Minecraft: seguro en cliente y servidor. */
public class HVData {
    public static final int READY = 75;
    public static final int MAX_FURY = 100;
    public static final int LEVEL2_DAMAGE = 500;
    public static final int LEVEL3_DAMAGE = 1200;

    public static final Codec<HVData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("forced", false).forGetter(d -> d.forced),
            Codec.BOOL.optionalFieldOf("last_active", false).forGetter(d -> d.lastActive),
            Codec.INT.optionalFieldOf("level", 1).forGetter(d -> d.level),
            Codec.INT.optionalFieldOf("damage", 0).forGetter(d -> d.damage),
            Codec.INT.optionalFieldOf("fury", 0).forGetter(d -> d.fury)
    ).apply(i, HVData::new));

    /** Activado por comando (/halfvastaya enable). */
    public boolean forced;
    /** Estaba activo la ultima vez (evita parpadeos al reconectar). */
    public boolean lastActive;
    public int level = 1;
    public int damage;
    public int fury;

    // --- estado temporal (no se guarda) ---
    public int originSeen = -1000;
    public int idle;
    public int decayCtr;
    public int lastCast = -1000;
    public boolean wasReady;

    public HVData() {
    }

    public HVData(boolean forced, boolean lastActive, int level, int damage, int fury) {
        this.forced = forced;
        this.lastActive = lastActive;
        this.level = Math.max(1, Math.min(3, level));
        this.damage = Math.max(0, damage);
        this.fury = Math.max(0, Math.min(MAX_FURY, fury));
    }

    public static double attackBonus(int level) {
        return level >= 3 ? 6.0 : level == 2 ? 2.5 : 0.0;
    }

    public static double armorBonus(int level) {
        return level >= 3 ? 7.0 : level == 2 ? 3.0 : 0.0;
    }

    public static String roman(int level) {
        return level >= 3 ? "III" : level == 2 ? "II" : "I";
    }
}
