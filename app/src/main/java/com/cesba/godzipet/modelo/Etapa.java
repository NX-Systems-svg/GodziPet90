package com.cesba.godzipet.modelo;

/** Etapas de vida. La etapa FINAL se combina con una {@link RutaEvolucion}. */
public enum Etapa {
    HUEVO("Huevo"),
    CRIA("Cría"),
    JUVENIL("Juvenil"),
    FINAL("Forma final");

    public final String nombre;

    Etapa(String nombre) {
        this.nombre = nombre;
    }
}
