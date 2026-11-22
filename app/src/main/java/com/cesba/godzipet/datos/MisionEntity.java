package com.cesba.godzipet.datos;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

/** Marca de recompensa reclamada (día para diarias, "LOGRO" para permanentes). */
@Entity(tableName = "misiones", primaryKeys = {"usuarioId", "misionId"})
public class MisionEntity {
    public long usuarioId;
    @NonNull public String misionId = "";
    @NonNull public String reclamadaEn = "";

    public MisionEntity() { }

    @Ignore
    public MisionEntity(long usuarioId, @NonNull String misionId, @NonNull String reclamadaEn) {
        this.usuarioId = usuarioId;
        this.misionId = misionId;
        this.reclamadaEn = reclamadaEn;
    }
}
