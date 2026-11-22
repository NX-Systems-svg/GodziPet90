package com.cesba.godzipet.modelo;

/**
 * Formas finales alternativas inspiradas en las eras del cine japonés.
 * Cada una modifica mecánicas del juego. La decide {@link Evolucion#decidir}.
 */
public enum RutaEvolucion {
    NINGUNA("—", "", "", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
    SHOWA("Godzilla Showa", "昭和",
            "+25% monedas en todo · la felicidad baja 50% más lento",
            1.25, 1.0, 1.0, 0.5, 1.0, 1.0),
    HEISEI("Godzilla Heisei", "平成",
            "Aliento Atómico carga x2 · entrenar cuesta 25% menos energía",
            1.0, 2.0, 1.0, 1.0, 0.75, 1.0),
    BURNING("Burning Godzilla", "バーニング",
            "Gana siempre las Alertas Kaiju y +50% XP en ellas · su radiación sube 50% más rápido",
            1.0, 1.0, 1.0, 1.0, 1.0, 1.5),
    SHIN("Shin Godzilla", "シン",
            "Forma secreta por descuido: +25% de experiencia · sus necesidades bajan 25% más rápido",
            1.0, 1.0, 1.25, 1.0, 1.0, 1.0);

    public final String nombre;
    public final String kanji;
    public final String ventaja;
    public final double multMonedas;
    public final double multCargaAliento;
    public final double multDeterioro;
    public final double multDeterioroFelicidad;
    public final double multCostoEntreno;
    public final double multRadiacion;

    RutaEvolucion(String nombre, String kanji, String ventaja, double multMonedas, double multCargaAliento,
                  double multDeterioro, double multDeterioroFelicidad, double multCostoEntreno,
                  double multRadiacion) {
        this.nombre = nombre;
        this.kanji = kanji;
        this.ventaja = ventaja;
        this.multMonedas = multMonedas;
        this.multCargaAliento = multCargaAliento;
        this.multDeterioro = multDeterioro;
        this.multDeterioroFelicidad = multDeterioroFelicidad;
        this.multCostoEntreno = multCostoEntreno;
        this.multRadiacion = multRadiacion;
    }

    public double multXp() {
        return this == SHIN ? 1.25 : 1.0;
    }

    /** Incluye compatibilidad con los nombres de la versión 1. */
    public static RutaEvolucion desde(String nombre) {
        if ("GUARDIAN".equals(nombre)) return SHOWA;
        if ("ATOMICO".equals(nombre)) return HEISEI;
        if ("SALVAJE".equals(nombre)) return SHIN;
        try {
            return valueOf(nombre);
        } catch (Exception e) {
            return NINGUNA;
        }
    }
}
