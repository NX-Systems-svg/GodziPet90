package com.cesba.godzipet.modelo;

/**
 * Los seis indicadores del kaiju (rango 0-100). La RADIACIÓN es la mecánica
 * característica: no se llena, se EQUILIBRA (zona ideal 40-70).
 */
public enum Stat {
    ALIMENTACION("Alimentación", "🍙"),
    FELICIDAD("Felicidad", "😊"),
    ENERGIA("Energía", "⚡"),
    HIGIENE("Higiene", "🫧"),
    SALUD("Salud", "❤"),
    RADIACION("Radiación", "☢");

    public final String nombre;
    public final String icono;

    Stat(String nombre, String icono) {
        this.nombre = nombre;
        this.icono = icono;
    }
}
