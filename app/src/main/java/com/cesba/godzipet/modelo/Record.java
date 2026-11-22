package com.cesba.godzipet.modelo;

/** Récord persistente de un minijuego. */
public final class Record {
    public int mejorPuntaje;
    public int victorias;
    public int jugadas;

    public Record(int mejorPuntaje, int victorias, int jugadas) {
        this.mejorPuntaje = mejorPuntaje;
        this.victorias = victorias;
        this.jugadas = jugadas;
    }
}
