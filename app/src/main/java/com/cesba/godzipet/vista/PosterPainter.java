package com.cesba.godzipet.vista;

import android.graphics.Bitmap;

/** CONTRATO (lo implementa el agente de UI): modo foto con marco de póster de película. */
public final class PosterPainter {
    private PosterPainter() { }

    /**
     * Devuelve un bitmap nuevo (vertical, ~1080x1350) con la escena dentro de un marco de póster
     * japonés: sol rojo con rayos, título ゴジラ, nombre del kaiju y su forma.
     */
    public static Bitmap crear(Bitmap escena, String nombreKaiju, String forma, String lugar) {
        return escena.copy(Bitmap.Config.ARGB_8888, false);
    }
}
