package com.cesba.godzipet.modelo;

/**
 * Reglas de tiempo real (tasas por HORA de juego, documentadas):
 * <pre>
 *  Alimentación  -8/h despierto, -4/h dormido
 *  Higiene       -5/h despierto, -2/h dormido
 *  Energía       -4/h despierto, +120/h dormido
 *  Felicidad     -4/h (-8/h si alimentación o higiene &lt; 30); a la mitad dormido
 *  Salud         -6/h si alimentación o higiene &lt; 20 o energía &lt; 10;
 *                +2/h si el promedio de bienestar ≥ 60
 * </pre>
 * Ejemplo comprobable: 80 de alimentación tras 2 h despierto → 64.
 * La ruta de evolución multiplica el deterioro (Salvaje x1.25, Guardián felicidad x0.5).
 */
public final class Deterioro {
    public static final double ALIM_HORA = 8, ALIM_HORA_DORMIDO = 4;
    public static final double HIGIENE_HORA = 5, HIGIENE_HORA_DORMIDO = 2;
    public static final double ENERGIA_HORA = 4, ENERGIA_RECUPERA_HORA = 120;
    public static final double FELICIDAD_HORA = 4, FELICIDAD_EXTRA_HORA = 4;
    public static final double SALUD_DANO_HORA = 6, SALUD_RECUPERA_HORA = 2;

    private Deterioro() { }

    public static void aplicar(Necesidades n, double minutos, boolean dormido, RutaEvolucion ruta) {
        if (minutos <= 0) return;
        double h = minutos / 60.0;
        double m = ruta.multDeterioro;

        boolean necesitado = n.get(Stat.ALIMENTACION) < 30 || n.get(Stat.HIGIENE) < 30;
        boolean critico = n.get(Stat.ALIMENTACION) < 20 || n.get(Stat.HIGIENE) < 20
                || n.get(Stat.ENERGIA) < 10;

        n.cambiar(Stat.ALIMENTACION, -(dormido ? ALIM_HORA_DORMIDO : ALIM_HORA) * h * m);
        n.cambiar(Stat.HIGIENE, -(dormido ? HIGIENE_HORA_DORMIDO : HIGIENE_HORA) * h * m);
        n.cambiar(Stat.ENERGIA, dormido ? ENERGIA_RECUPERA_HORA * h : -ENERGIA_HORA * h * m);

        double feliz = FELICIDAD_HORA + (necesitado ? FELICIDAD_EXTRA_HORA : 0);
        if (dormido) feliz /= 2;
        n.cambiar(Stat.FELICIDAD, -feliz * h * m * ruta.multDeterioroFelicidad);

        if (critico) n.cambiar(Stat.SALUD, -SALUD_DANO_HORA * h * m);
        else if (n.promedio() >= 60) n.cambiar(Stat.SALUD, SALUD_RECUPERA_HORA * h);
    }
}
