package com.cesba.godzipet.datos;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {UsuarioEntity.class, PartidaEntity.class, InventarioEntity.class,
        DecoracionEntity.class, MisionEntity.class, RecordEntity.class}, version = 1, exportSchema = true)
public abstract class BaseDatos extends RoomDatabase {
    private static volatile BaseDatos instancia;

    public abstract JuegoDao dao();

    public static BaseDatos get(Context ctx) {
        if (instancia == null) {
            synchronized (BaseDatos.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(ctx.getApplicationContext(), BaseDatos.class, "godzipet.db")
                            .build();
                }
            }
        }
        return instancia;
    }
}
