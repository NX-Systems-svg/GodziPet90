package com.cesba.godzipet.vista;

import java.util.Calendar;

/** Clima según la estación real del año (hemisferio norte, como en Japón). */
public enum Clima {
    SAKURA, SOL, LLUVIA, NIEVE;

    public static Clima actual() {
        int mes = Calendar.getInstance().get(Calendar.MONTH) + 1;
        if (mes >= 3 && mes <= 5) return SAKURA;
        if (mes >= 6 && mes <= 8) return SOL;
        if (mes >= 9 && mes <= 11) return LLUVIA;
        return NIEVE;
    }
}
