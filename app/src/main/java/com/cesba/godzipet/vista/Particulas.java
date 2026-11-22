package com.cesba.godzipet.vista;

import android.graphics.Canvas;

/**
 * CONTRATO (lo implementa el agente de UI): sistema de partículas con pool (sin crear
 * objetos por cuadro). Las vistas llaman a {@link #dibujar} en cada onDraw.
 */
public final class Particulas {
    public enum Tipo { ESCOMBRO, CHISPA, HUMO, PETALO, ATOMICO, MONEDA, CORAZON, BURBUJA, NIEVE, LLUVIA, VAPOR }

    /** Emite {@code n} partículas en (x, y). {@code escala} = px por unidad (tamaño relativo). */
    public void emitir(Tipo tipo, float x, float y, int n, float escala) { }

    /** Texto flotante que sube y se desvanece (p. ej. "+10 🪙"). */
    public void texto(String s, float x, float y, int color, float tamPx) { }

    /** Avanza la simulación dt segundos y dibuja. */
    public void dibujar(Canvas c, float dt) { }

    public boolean activas() { return false; }

    public void limpiar() { }
}
