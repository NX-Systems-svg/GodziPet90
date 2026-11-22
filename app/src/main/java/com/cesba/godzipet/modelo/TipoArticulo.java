package com.cesba.godzipet.modelo;

public enum TipoArticulo {
    COMIDA("Comida"), CUIDADO("Cuidado"), DECORACION("Decoración");

    public final String nombre;

    TipoArticulo(String nombre) { this.nombre = nombre; }
}
