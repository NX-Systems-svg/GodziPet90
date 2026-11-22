package com.cesba.godzipet.modelo;

/**
 * Estadísticas de bienestar. Toda modificación pasa por {@link #cambiar}
 * que garantiza que ningún valor salga del rango [0, 100].
 */
public final class Necesidades {
    public static final double MIN = 0;
    public static final double MAX = 100;
    public static final int CRITICO = 20;

    private final double[] valores = new double[Stat.values().length];

    public Necesidades(double alimentacion, double felicidad, double energia,
                       double higiene, double salud) {
        establecer(Stat.ALIMENTACION, alimentacion);
        establecer(Stat.FELICIDAD, felicidad);
        establecer(Stat.ENERGIA, energia);
        establecer(Stat.HIGIENE, higiene);
        establecer(Stat.SALUD, salud);
    }

    public static Necesidades iniciales() {
        return new Necesidades(80, 80, 80, 80, 100);
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

    public double promedio() {
        double suma = 0;
        for (double v : valores) suma += v;
        return suma / valores.length;
    }

    public boolean esCritico(Stat s) {
        return get(s) < CRITICO;
    }

    /** Devuelve el indicador más bajo si está en nivel crítico, o null. */
    public Stat peorCritico() {
        Stat peor = null;
        for (Stat s : Stat.values()) {
            if (esCritico(s) && (peor == null || get(s) < get(peor))) peor = s;
        }
        return peor;
    }

    public Necesidades copia() {
        return new Necesidades(get(Stat.ALIMENTACION), get(Stat.FELICIDAD),
                get(Stat.ENERGIA), get(Stat.HIGIENE), get(Stat.SALUD));
    }
}
