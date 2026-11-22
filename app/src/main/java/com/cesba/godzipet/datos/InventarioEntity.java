package com.cesba.godzipet.datos;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(tableName = "inventario", primaryKeys = {"usuarioId", "articuloId"})
public class InventarioEntity {
    public long usuarioId;
    @NonNull public String articuloId = "";
    public int cantidad;

    public InventarioEntity() { }

    @Ignore
    public InventarioEntity(long usuarioId, @NonNull String articuloId, int cantidad) {
        this.usuarioId = usuarioId;
        this.articuloId = articuloId;
        this.cantidad = cantidad;
    }
}
