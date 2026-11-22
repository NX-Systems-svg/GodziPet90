package com.cesba.godzipet.vista;

/** Contrato compartido por las vistas de escenario: proporciones de la escena principal. */
public final class Escenas {
    /** Fracción de la altura donde termina el cielo/horizonte. */
    public static final float HORIZONTE = 0.50f;
    /** Fracción de la altura donde empieza el suelo firme (playa, calle, muelle…). */
    public static final float INICIO_SUELO = 0.74f;
    /** Fracción de la altura donde se apoyan los pies del kaiju. */
    public static final float PIES = 0.93f;

    private Escenas() { }
}
