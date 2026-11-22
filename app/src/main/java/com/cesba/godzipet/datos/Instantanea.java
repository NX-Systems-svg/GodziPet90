package com.cesba.godzipet.datos;

import java.util.List;

/** Copia de la partida lista para escribirse en otro hilo sin compartir objetos mutables. */
public final class Instantanea {
    public final PartidaEntity partida;
    public final List<InventarioEntity> inventario;
    public final List<DecoracionEntity> decoraciones;
    public final List<MisionEntity> misiones;
    public final List<RecordEntity> records;

    public Instantanea(PartidaEntity partida, List<InventarioEntity> inventario,
                       List<DecoracionEntity> decoraciones, List<MisionEntity> misiones,
                       List<RecordEntity> records) {
        this.partida = partida;
        this.inventario = inventario;
        this.decoraciones = decoraciones;
        this.misiones = misiones;
        this.records = records;
    }
}
