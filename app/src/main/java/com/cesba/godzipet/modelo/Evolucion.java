package com.cesba.godzipet.modelo;

/**
 * Regla medible de evolución final (se evalúa al llegar a nivel 5 siendo Juvenil):
 * <ol>
 *   <li>Promedio histórico de bienestar &lt; 50 → <b>Salvaje</b> (fue descuidado).</li>
 *   <li>Entrenamientos ≥ 6 y entrenamientos × 2 ≥ cuidados → <b>Atómico</b> (hábito de entrenar).</li>
 *   <li>En otro caso → <b>Guardián</b> (cuidado constante).</li>
 * </ol>
 * "Cuidados" = alimentar + jugar + dormir + bañar + curar.
 */
public final class Evolucion {
    public static final double UMBRAL_DESCUIDO = 50;
    public static final int ENTRENOS_MINIMOS = 6;
    public static final int NIVEL_JUVENIL = 3;
    public static final int NIVEL_FINAL = 5;

    private Evolucion() { }

    public static RutaEvolucion decidir(double promedioBienestar, int entrenamientos, int cuidados) {
        if (promedioBienestar < UMBRAL_DESCUIDO) return RutaEvolucion.SALVAJE;
        if (entrenamientos >= ENTRENOS_MINIMOS && entrenamientos * 2 >= cuidados) {
            return RutaEvolucion.ATOMICO;
        }
        return RutaEvolucion.GUARDIAN;
    }

    /** Explicación legible de por qué se elegiría esa ruta. */
    public static String explicar(double promedio, int entrenos, int cuidados) {
        return String.format(java.util.Locale.US,
                "Bienestar promedio %.0f (mín. 50) · Entrenamientos %d (mín. %d) · Cuidados %d",
                promedio, entrenos, ENTRENOS_MINIMOS, cuidados);
    }
}
