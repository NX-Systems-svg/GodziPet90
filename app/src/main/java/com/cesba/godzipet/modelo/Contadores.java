package com.cesba.godzipet.modelo;

/**
 * Historial de hábitos (totales y del día). Lo usan las misiones y la regla de evolución.
 * Es un contenedor de datos simple que se persiste tal cual.
 */
public final class Contadores {
    // Totales (nunca se reinician)
    public int alimentar, jugar, dormir, banar, curar, entrenar, centrales;
    public int minijuegos, victorias, defensas, alientos;
    // Historial para la evolución (se reinicia con cada generación)
    public double sumaBienestar, minutosBienestar, sumaRadiacion;
    public int entrenosBase, cuidadosBase;
    // Contadores diarios
    public String diaClave = "";
    public int hoyAlimentar, hoyMinijuegos, hoyEntrenar;
    public double hoyMinutosZona;

    public int cuidadosTotales() {
        return alimentar + jugar + dormir + banar + curar;
    }

    public int entrenosGeneracion() {
        return entrenar - entrenosBase;
    }

    public int cuidadosGeneracion() {
        return cuidadosTotales() - cuidadosBase;
    }

    public void muestrear(double promedio, double radiacion, double minutos, boolean enZona) {
        sumaBienestar += promedio * minutos;
        sumaRadiacion += radiacion * minutos;
        minutosBienestar += minutos;
        if (enZona) hoyMinutosZona += minutos;
    }

    public double promedioRadiacion(double actual) {
        return minutosBienestar > 0 ? sumaRadiacion / minutosBienestar : actual;
    }

    /** Nueva generación: el historial de evolución empieza de cero. */
    public void nuevaGeneracion() {
        sumaBienestar = 0;
        minutosBienestar = 0;
        sumaRadiacion = 0;
        entrenosBase = entrenar;
        cuidadosBase = cuidadosTotales();
    }

    /** Renueva los contadores diarios si cambió el día. Devuelve true si se renovó. */
    public boolean renovarDia(String hoy) {
        if (hoy.equals(diaClave)) return false;
        diaClave = hoy;
        hoyAlimentar = 0;
        hoyMinijuegos = 0;
        hoyEntrenar = 0;
        hoyMinutosZona = 0;
        return true;
    }
}
