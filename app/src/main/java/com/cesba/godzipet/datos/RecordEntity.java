package com.cesba.godzipet.datos;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(tableName = "records", primaryKeys = {"usuarioId", "juegoId"})
public class RecordEntity {
    public long usuarioId;
    @NonNull public String juegoId = "";
    public int mejorPuntaje;
    public int victorias;
    public int jugadas;

    public RecordEntity() { }

    @Ignore
    public RecordEntity(long usuarioId, @NonNull String juegoId, int mejor, int victorias, int jugadas) {
        this.usuarioId = usuarioId;
        this.juegoId = juegoId;
        this.mejorPuntaje = mejor;
        this.victorias = victorias;
        this.jugadas = jugadas;
    }
}
