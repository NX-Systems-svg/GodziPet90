package com.cesba.godzipet.modelo;

/** Los cinco indicadores de bienestar del kaiju (rango 0-100). */
public enum Stat {
    ALIMENTACION("Alimentación", "🐟"),
    FELICIDAD("Felicidad", "😊"),
    ENERGIA("Energía", "⚡"),
    HIGIENE("Higiene", "🫧"),
    SALUD("Salud", "❤");

    public final String nombre;
    public final String icono;

    Stat(String nombre, String icono) {
        this.nombre = nombre;
        this.icono = icono;
    }
}
