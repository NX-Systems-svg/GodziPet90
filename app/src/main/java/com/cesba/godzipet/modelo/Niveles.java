package com.cesba.godzipet.modelo;

/** Progresión de 6 niveles por experiencia acumulada. */
public final class Niveles {
    /** XP mínima para cada nivel (índice 0 = nivel 1). */
    public static final int[] UMBRALES = {0, 60, 150, 280, 450, 700};
    public static final int MAXIMO = UMBRALES.length;

    public static final String[] DESBLOQUEOS = {
            "Isla Odo, Bahía de Tokio, Defensa de Tokio y Anillos de Humo",
            "Evoluciona a Little Godzilla · Memoria G-Force · Ginza 1954 · Monte Fuji",
            "Evoluciona a Godzilla Junior · Aliento Atómico real · Alertas Kaiju · Torre de Tokio",
            "Castillo de Osaka · Torre de Tokio en miniatura",
            "¡Forma final según tu forma de cuidarlo! · Kamakura",
            "Isla de los Monstruos · Título Rey de los Monstruos (+10 monedas por misión)"
    };

    private Niveles() { }

    public static int nivelPara(int xp) {
        int nivel = 1;
        for (int i = 0; i < UMBRALES.length; i++) if (xp >= UMBRALES[i]) nivel = i + 1;
        return nivel;
    }

    public static int xpBase(int nivel) {
        return UMBRALES[Math.max(0, Math.min(MAXIMO, nivel) - 1)];
    }

    public static int xpSiguiente(int nivel) {
        return nivel >= MAXIMO ? -1 : UMBRALES[nivel];
    }

    public static String desbloqueo(int nivel) {
        return DESBLOQUEOS[Math.max(1, Math.min(MAXIMO, nivel)) - 1];
    }
}
