package com.cesba.godzipet.modelo;

/** Artículo del catálogo (inmutable). */
public final class Articulo {
    public final String id;
    public final String nombre;
    public final String descripcion;
    public final String icono;
    public final int precio;
    public final TipoArticulo tipo;
    public final Slot slot;          // sólo decoraciones
    public final int nivelRequerido;
    // Efectos al consumir (comida / cuidado)
    public final int alimentacion, felicidad, energia, higiene, salud, aliento;

    private Articulo(String id, String nombre, String descripcion, String icono, int precio,
                     TipoArticulo tipo, Slot slot, int nivelRequerido,
                     int alimentacion, int felicidad, int energia, int higiene, int salud, int aliento) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.icono = icono;
        this.precio = precio;
        this.tipo = tipo;
        this.slot = slot;
        this.nivelRequerido = nivelRequerido;
        this.alimentacion = alimentacion;
        this.felicidad = felicidad;
        this.energia = energia;
        this.higiene = higiene;
        this.salud = salud;
        this.aliento = aliento;
    }

    static Articulo consumible(String id, String nombre, String desc, String icono, int precio,
                               TipoArticulo tipo, int nivel, int alim, int feliz, int energia,
                               int higiene, int salud, int aliento) {
        return new Articulo(id, nombre, desc, icono, precio, tipo, null, nivel,
                alim, feliz, energia, higiene, salud, aliento);
    }

    static Articulo decoracion(String id, String nombre, String desc, String icono, int precio,
                               Slot slot, int nivel) {
        return new Articulo(id, nombre, desc, icono, precio, TipoArticulo.DECORACION, slot, nivel,
                0, 0, 0, 0, 0, 0);
    }

    public boolean esConsumible() {
        return tipo != TipoArticulo.DECORACION;
    }

    /** Texto corto de efectos, p. ej. "+25 🐟  +5 😊". */
    public String resumenEfectos() {
        StringBuilder sb = new StringBuilder();
        agregar(sb, alimentacion, Stat.ALIMENTACION.icono);
        agregar(sb, felicidad, Stat.FELICIDAD.icono);
        agregar(sb, energia, Stat.ENERGIA.icono);
        agregar(sb, higiene, Stat.HIGIENE.icono);
        agregar(sb, salud, Stat.SALUD.icono);
        agregar(sb, aliento, "☢");
        return sb.toString().trim();
    }

    private static void agregar(StringBuilder sb, int v, String icono) {
        if (v != 0) sb.append(v > 0 ? "+" : "").append(v).append(' ').append(icono).append("  ");
    }
}
