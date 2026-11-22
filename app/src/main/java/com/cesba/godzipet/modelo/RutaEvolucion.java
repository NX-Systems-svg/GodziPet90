package com.cesba.godzipet.modelo;

/**
 * Evoluciones finales alternativas. Cada una modifica mecánicas del juego.
 * La ruta la decide {@link Evolucion#decidir} según el historial de cuidado.
 */
public enum RutaEvolucion {
    NINGUNA("—", "", 1.0, 1.0, 1.0, 1.0, 1.0),
    GUARDIAN("Godzilla Guardián",
            "+25% monedas en todo · la felicidad baja 50% más lento",
            1.25, 1.0, 1.0, 0.5, 1.0),
    ATOMICO("Godzilla Atómico",
            "Aliento Atómico carga x2 · entrenar cuesta 25% menos energía",
            1.0, 2.0, 1.0, 1.0, 0.75),
    SALVAJE("Godzilla Salvaje",
            "Gana siempre las Alertas Kaiju y +50% XP en ellas · sus necesidades bajan 25% más rápido",
            1.0, 1.0, 1.25, 1.0, 1.0);

    public final String nombre;
    public final String ventaja;
    public final double multMonedas;
    public final double multCargaAliento;
    public final double multDeterioro;
    public final double multDeterioroFelicidad;
    public final double multCostoEntreno;

    RutaEvolucion(String nombre, String ventaja, double multMonedas, double multCargaAliento,
                  double multDeterioro, double multDeterioroFelicidad, double multCostoEntreno) {
        this.nombre = nombre;
        this.ventaja = ventaja;
        this.multMonedas = multMonedas;
        this.multCargaAliento = multCargaAliento;
        this.multDeterioro = multDeterioro;
        this.multDeterioroFelicidad = multDeterioroFelicidad;
        this.multCostoEntreno = multCostoEntreno;
    }

    public static RutaEvolucion desde(String nombre) {
        try {
            return valueOf(nombre);
        } catch (Exception e) {
            return NINGUNA;
        }
    }
}
