package com.cesba.godzipet.modelo;

/**
 * Etapas de vida fieles a la línea Heisei de las películas:
 * Baby Godzilla (1993) → Little Godzilla (1994) → Godzilla Junior (1995) → forma final.
 */
public enum Etapa {
    HUEVO("Huevo", "Huevo"),
    BEBE("Baby Godzilla", "Baby"),
    PEQUENO("Little Godzilla", "Little"),
    JUNIOR("Godzilla Junior", "Junior"),
    FINAL("Forma final", "Final");

    public final String nombre;
    public final String corto;

    Etapa(String nombre, String corto) {
        this.nombre = nombre;
        this.corto = corto;
    }

    /** Compatibilidad con partidas guardadas en la versión 1. */
    public static Etapa desde(String s) {
        if ("CRIA".equals(s)) return BEBE;
        if ("JUVENIL".equals(s)) return JUNIOR;
        try {
            return valueOf(s);
        } catch (Exception e) {
            return HUEVO;
        }
    }
}
