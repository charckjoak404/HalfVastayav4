package com.halfvastaya;

/** Copia en el cliente de los datos del jugador (sin clases de Minecraft). */
public final class ClientState {
    public static boolean active;
    public static int level = 1;
    public static int damage;
    public static int fury;

    public static void apply(boolean a, int l, int d, int f) {
        active = a;
        level = l;
        damage = d;
        fury = f;
    }

    public static void reset() {
        apply(false, 1, 0, 0);
    }

    private ClientState() {
    }
}
