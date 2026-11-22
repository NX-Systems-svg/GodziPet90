package com.cesba.godzipet.modelo;

/** Posiciones definidas de la guarida donde se equipan decoraciones. */
public enum Slot {
    VENTANA("Ventanal", "Lo que se ve por la ventana"),
    IZQUIERDA("Rincón izquierdo", "Objeto grande a la izquierda"),
    DERECHA("Rincón derecho", "Objeto grande a la derecha"),
    SUELO("Suelo", "Alfombra o detalle del piso");

    public final String nombre;
    public final String descripcion;

    Slot(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }
}
