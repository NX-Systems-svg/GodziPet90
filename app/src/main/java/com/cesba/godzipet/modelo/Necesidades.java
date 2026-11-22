package com.cesba.godzipet.modelo;

/**
 * Estadísticas de bienestar. Toda modificación pasa por {@link #cambiar}
 * que garantiza que ningún valor salga del rango [0, 100].
 * La radiación se evalúa por cercanía a su zona ideal, no por "más es mejor".
 */
public final class Necesidades {
    public static final double MIN = 0;
    public static final double MAX = 100;
    public static final int CRITICO = 20;
    public static final int RAD_IDEAL_MIN = 40;
    public static final int RAD_IDEAL_MAX = 70;
    public static final int RAD_BAJA = 20;
    public static final int RAD_ALTA = 85;
    public static final int RAD_FUSION = 95;

    private final double[] valores = new double[Stat.values().length];

    public Necesidades(double alimentacion, double felicidad, double energia,
                       double higiene, double salud, double radiacion) {
        establecer(Stat.ALIMENTACION, alimentacion);
        establecer(Stat.FELICIDAD, felicidad);
        establecer(Stat.ENERGIA, energia);
        establecer(Stat.HIGIENE, higiene);
        establecer(Stat.SALUD, salud);
        establecer(Stat.RADIACION, radiacion);
    }

    public static Necesidades iniciales() {
        return new Necesidades(80, 80, 80, 80, 100, 55);
    }

    public static double limitar(double v) {
        if (Double.isNaN(v)) return MIN;
        return Math.max(MIN, Math.min(MAX, v));
    }

    public double get(Stat s) {
        return valores[s.ordinal()];
    }

    /** Valor redondeado para mostrar en pantalla. */
    public int valor(Stat s) {
        return (int) Math.round(valores[s.ordinal()]);
    }

    public void establecer(Stat s, double v) {
        valores[s.ordinal()] = limitar(v);
    }

    public void cambiar(Stat s, double delta) {
        establecer(s, get(s) + delta);
    }

    /** Calidad 0-100 de un indicador: la radiación vale 100 dentro de la zona ideal. */
    public double calidad(Stat s) {
        if (s != Stat.RADIACION) return get(s);
        double r = get(s);
        if (r >= RAD_IDEAL_MIN && r <= RAD_IDEAL_MAX) return 100;
        double distancia = r < RAD_IDEAL_MIN ? RAD_IDEAL_MIN - r : r - RAD_IDEAL_MAX;
        return limitar(100 - distancia * 2.5);
    }

    public double promedio() {
        double suma = 0;
        for (Stat s : Stat.values()) suma += calidad(s);
        return suma / valores.length;
    }

    public boolean enZonaIdeal() {
        double r = get(Stat.RADIACION);
        return r >= RAD_IDEAL_MIN && r <= RAD_IDEAL_MAX;
    }

    public boolean esCritico(Stat s) {
        if (s == Stat.RADIACION) return get(s) < RAD_BAJA || get(s) >= RAD_ALTA;
        return get(s) < CRITICO;
    }

    /** Devuelve el indicador en peor situación si está en nivel crítico, o null. */
    public Stat peorCritico() {
        Stat peor = null;
        for (Stat s : Stat.values()) {
            if (esCritico(s) && (peor == null || calidad(s) < calidad(peor))) peor = s;
        }
        return peor;
    }

    public Necesidades copia() {
        return new Necesidades(get(Stat.ALIMENTACION), get(Stat.FELICIDAD), get(Stat.ENERGIA),
                get(Stat.HIGIENE), get(Stat.SALUD), get(Stat.RADIACION));
    }
}
