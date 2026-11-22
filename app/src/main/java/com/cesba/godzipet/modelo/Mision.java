package com.cesba.godzipet.modelo;

import java.util.function.ToIntFunction;

/** Definición de una misión: detecta acciones reales leyendo el estado de la partida. */
public final class Mision {
    public final String id;
    public final String titulo;
    public final String descripcion;
    public final boolean diaria;
    public final int meta;
    public final int recompensaMonedas;
    public final int recompensaXp;
    private final ToIntFunction<Partida> medidor;

    public Mision(String id, String titulo, String descripcion, boolean diaria, int meta,
                  int recompensaMonedas, int recompensaXp, ToIntFunction<Partida> medidor) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.diaria = diaria;
        this.meta = meta;
        this.recompensaMonedas = recompensaMonedas;
        this.recompensaXp = recompensaXp;
        this.medidor = medidor;
    }

    public int progreso(Partida p) {
        return Math.min(meta, Math.max(0, medidor.applyAsInt(p)));
    }

    public boolean completada(Partida p) {
        return progreso(p) >= meta;
    }
}
