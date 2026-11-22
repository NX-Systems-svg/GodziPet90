package com.cesba.godzipet.modelo;

/**
 * Regla medible de evolución final (se evalúa al llegar a nivel 5 siendo Godzilla Junior):
 * <ol>
 *   <li>Bienestar promedio &lt; 45 → <b>Shin Godzilla</b> (forma secreta por descuido).</li>
 *   <li>Radiación promedio ≥ 72 → <b>Burning Godzilla</b> (vivió sobrecalentado).</li>
 *   <li>Entrenamientos ≥ 6 y entrenamientos × 2 ≥ cuidados → <b>Godzilla Heisei</b> (guerrero).</li>
 *   <li>En otro caso → <b>Godzilla Showa</b> (cuidado equilibrado, héroe).</li>
 * </ol>
 * Los promedios están ponderados por minuto de juego y se cuentan desde la generación actual.
 */
public final class Evolucion {
    public static final double UMBRAL_DESCUIDO = 45;
    public static final double UMBRAL_BURNING = 72;
    public static final int ENTRENOS_MINIMOS = 6;
    public static final int NIVEL_PEQUENO = 2;
    public static final int NIVEL_JUNIOR = 3;
    public static final int NIVEL_FINAL = 5;

    private Evolucion() { }

    public static RutaEvolucion decidir(double promedioBienestar, double promedioRadiacion,
                                        int entrenamientos, int cuidados) {
        if (promedioBienestar < UMBRAL_DESCUIDO) return RutaEvolucion.SHIN;
        if (promedioRadiacion >= UMBRAL_BURNING) return RutaEvolucion.BURNING;
        if (entrenamientos >= ENTRENOS_MINIMOS && entrenamientos * 2 >= cuidados) return RutaEvolucion.HEISEI;
        return RutaEvolucion.SHOWA;
    }

    /** Etapa correspondiente a un nivel (sin contar el huevo ni la forma final). */
    public static Etapa etapaPorNivel(int nivel) {
        if (nivel >= NIVEL_JUNIOR) return Etapa.JUNIOR;
        if (nivel >= NIVEL_PEQUENO) return Etapa.PEQUENO;
        return Etapa.BEBE;
    }
}
