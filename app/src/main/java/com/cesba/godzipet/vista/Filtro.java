package com.cesba.godzipet.vista;

/** Filtros visuales de pantalla completa. */
public enum Filtro {
    NINGUNO("Normal"), B_N_1954("Película 1954 (blanco y negro)"), POSTER("Póster retro"), CRT("Televisor CRT");

    public final String nombre;

    Filtro(String nombre) { this.nombre = nombre; }

    public static Filtro desde(String s) {
        try {
            return valueOf(s);
        } catch (Exception e) {
            return NINGUNO;
        }
    }
}
