package com.cesba.godzipet.modelo;

/** Resultado de una operación del juego: éxito/error + mensaje + animación. */
public final class Resultado {
    public final boolean ok;
    public final String mensaje;
    public final Reaccion reaccion;

    private Resultado(boolean ok, String mensaje, Reaccion reaccion) {
        this.ok = ok;
        this.mensaje = mensaje;
        this.reaccion = reaccion;
    }

    public static Resultado exito(String mensaje, Reaccion reaccion) {
        return new Resultado(true, mensaje, reaccion);
    }

    public static Resultado error(String mensaje) {
        return new Resultado(false, mensaje, Reaccion.ERROR);
    }
}
