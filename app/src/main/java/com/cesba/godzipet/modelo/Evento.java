package com.cesba.godzipet.modelo;

/** Evento ocurrido durante la actualización del juego (para mostrar en la interfaz). */
public final class Evento {
    public enum Tipo { SUBIO_NIVEL, ECLOSION, EVOLUCION, DESPERTO, ALERTA_NUEVA, ALERTA_FALLIDA, DIA_NUEVO }

    public final Tipo tipo;
    public final String mensaje;

    public Evento(Tipo tipo, String mensaje) {
        this.tipo = tipo;
        this.mensaje = mensaje;
    }
}
