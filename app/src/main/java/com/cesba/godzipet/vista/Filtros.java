package com.cesba.godzipet.vista;

import android.graphics.Canvas;

/**
 * CONTRATO (lo implementa el agente de UI): aplica un filtro de pantalla completa.
 * Uso: {@code int s = Filtros.inicio(c, w, h, f); ...dibujar escena...; Filtros.fin(c, s, w, h, f, t);}
 */
public final class Filtros {
    private Filtros() { }

    /** Abre una capa si el filtro lo necesita (blanco y negro / sepia). Devuelve el id de save. */
    public static int inicio(Canvas c, float w, float h, Filtro f) {
        return c.save();
    }

    /** Cierra la capa y dibuja grano, viñeta o líneas de barrido. t = segundos. */
    public static void fin(Canvas c, int save, float w, float h, Filtro f, float t) {
        c.restoreToCount(save);
    }
}
