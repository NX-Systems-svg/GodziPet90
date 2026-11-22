package com.cesba.godzipet.datos;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

/** Decoración equipada en una posición (slot) de la guarida. */
@Entity(tableName = "decoraciones", primaryKeys = {"usuarioId", "slot"})
public class DecoracionEntity {
    public long usuarioId;
    @NonNull public String slot = "";
    @NonNull public String articuloId = "";

    public DecoracionEntity() { }

    @Ignore
    public DecoracionEntity(long usuarioId, @NonNull String slot, @NonNull String articuloId) {
        this.usuarioId = usuarioId;
        this.slot = slot;
        this.articuloId = articuloId;
    }
}
