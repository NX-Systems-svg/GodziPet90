package com.cesba.godzipet.modelo;

/** Selección de personaje: origen del huevo con configuración inicial distinta. */
public enum OrigenHuevo {
    ISLA_ODO("Huevo de la Isla Odo",
            "De la aldea pesquera donde Godzilla fue visto por primera vez en 1954. Lo cuidan los pescadores.",
            "+60 monedas iniciales", 0xFF7D8A6A, 0xFF3F4A35, 60, 0, 0, 55),
    ADONOA("Huevo de la Isla Adonoa",
            "Hallado junto a un nido de pteranodón, como el de Baby Godzilla. Nace bañado en radiación.",
            "Aliento al 50% y radiación inicial alta", 0xFF8FBF6A, 0xFF4A7A2E, 0, 50, 0, 68),
    ISLA_NACIMIENTO("Huevo de la Isla del Nacimiento",
            "De la isla volcánica donde creció Little Godzilla. Es fuerte y resistente.",
            "+2 botiquines y energía al 100", 0xFF9A7462, 0xFF5B3E33, 0, 0, 2, 55),
    NUEVA_YORK("Huevo de Nueva York",
            "Easter egg: homenaje a la caricatura de 1998 en Manhattan. Tendrá rayas granate como en la serie.",
            "+30 monedas y estilo de la serie animada", 0xFF6F8F9A, 0xFF34505C, 30, 20, 0, 55);

    public final String nombre;
    public final String descripcion;
    public final String bono;
    public final int colorClaro;
    public final int colorOscuro;
    public final int monedasExtra;
    public final int alientoInicial;
    public final int botiquinesExtra;
    public final int radiacionInicial;

    OrigenHuevo(String nombre, String descripcion, String bono, int colorClaro, int colorOscuro,
                int monedasExtra, int alientoInicial, int botiquinesExtra, int radiacionInicial) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.bono = bono;
        this.colorClaro = colorClaro;
        this.colorOscuro = colorOscuro;
        this.monedasExtra = monedasExtra;
        this.alientoInicial = alientoInicial;
        this.botiquinesExtra = botiquinesExtra;
        this.radiacionInicial = radiacionInicial;
    }

    /** Incluye compatibilidad con los nombres de la versión 1. */
    public static OrigenHuevo desde(String nombre) {
        if ("MANHATTAN".equals(nombre)) return NUEVA_YORK;
        if ("POLINESIA".equals(nombre)) return ADONOA;
        if ("ISLA_MONSTRUO".equals(nombre)) return ISLA_NACIMIENTO;
        try {
            return valueOf(nombre);
        } catch (Exception e) {
            return ISLA_ODO;
        }
    }
}
