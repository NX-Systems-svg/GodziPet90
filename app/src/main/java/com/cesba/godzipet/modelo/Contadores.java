package com.cesba.godzipet.modelo;

/**
 * Historial de hábitos (totales y del día). Lo usan las misiones y la regla de evolución.
 * Es un contenedor de datos simple que se persiste tal cual.
 */
public final class Contadores {
    // Totales (nunca se reinician)
    public int alimentar, jugar, dormir, banar, curar, entrenar;
    public int minijuegos, victorias, defensas, alientos;
    // Promedio de bienestar ponderado por minutos de juego
    public double sumaBienestar, minutosBienestar;
    // Contadores diarios
    public String diaClave = "";
    public int hoyAlimentar, hoyMinijuegos, hoyEntrenar;

    public int cuidadosTotales() {
        return alimentar + jugar + dormir + banar + curar;
    }

    public void muestrearBienestar(double promedio, double minutos) {
        sumaBienestar += promedio * minutos;
        minutosBienestar += minutos;
    }

    /** Renueva los contadores diarios si cambió el día. Devuelve true si se renovó. */
    public boolean renovarDia(String hoy) {
        if (hoy.equals(diaClave)) return false;
        diaClave = hoy;
        hoyAlimentar = 0;
        hoyMinijuegos = 0;
        hoyEntrenar = 0;
        return true;
    }
}
