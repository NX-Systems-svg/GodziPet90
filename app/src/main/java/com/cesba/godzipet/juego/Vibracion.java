package com.cesba.godzipet.juego;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** Vibraciones con ritmo: pisadas, rugido y ronroneo. Respeta la preferencia del jugador. */
public final class Vibracion {
    private Vibracion() { }

    private static Vibrator vibrador(Context c) {
        if (!Juego.get(c).prefs().vibracion()) return null;
        Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        return v != null && v.hasVibrator() ? v : null;
    }

    private static void patron(Context c, long[] tiempos, int[] fuerzas) {
        Vibrator v = vibrador(c);
        if (v == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createWaveform(tiempos, fuerzas, -1));
            } else {
                v.vibrate(tiempos, -1);
            }
        } catch (RuntimeException e) {
            android.util.Log.w("Vibracion", "No se pudo vibrar", e);
        }
    }

    /** Un paso pesado del kaiju. */
    public static void pisada(Context c) {
        patron(c, new long[]{0, 35}, new int[]{0, 90});
    }

    /** Terremoto: tres pisotones. */
    public static void terremoto(Context c) {
        patron(c, new long[]{0, 120, 80, 140, 70, 200}, new int[]{0, 255, 0, 230, 0, 255});
    }

    /** Rugido: vibración creciente. */
    public static void rugido(Context c) {
        patron(c, new long[]{0, 120, 120, 160, 300}, new int[]{0, 70, 130, 190, 255});
    }

    /** Ronroneo al acariciarlo. */
    public static void ronroneo(Context c) {
        patron(c, new long[]{0, 40, 60, 40, 60, 40, 60, 40}, new int[]{0, 60, 0, 70, 0, 60, 0, 50});
    }
}
