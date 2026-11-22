package com.cesba.godzipet.datos;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {UsuarioEntity.class, PartidaEntity.class, InventarioEntity.class,
        DecoracionEntity.class, MisionEntity.class, RecordEntity.class}, version = 2, exportSchema = true)
public abstract class BaseDatos extends RoomDatabase {
    private static volatile BaseDatos instancia;

    public abstract JuegoDao dao();

    /** v1 → v2: agrega radiación, fusión, generación, escenario y Kaiju-pedia sin borrar partidas. */
    static final Migration MIGRACION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            String[] columnas = {
                    "radiacion REAL NOT NULL DEFAULT 55",
                    "fusion REAL NOT NULL DEFAULT 0",
                    "generacion INTEGER NOT NULL DEFAULT 1",
                    "escenario TEXT",
                    "ultimaCentral INTEGER NOT NULL DEFAULT 0",
                    "cCentrales INTEGER NOT NULL DEFAULT 0",
                    "sumaRadiacion REAL NOT NULL DEFAULT 0",
                    "entrenosBase INTEGER NOT NULL DEFAULT 0",
                    "cuidadosBase INTEGER NOT NULL DEFAULT 0",
                    "hoyMinutosZona REAL NOT NULL DEFAULT 0",
                    "secretos TEXT", "visitados TEXT", "formas TEXT"};
            for (String c : columnas) db.execSQL("ALTER TABLE partidas ADD COLUMN " + c);
            // las partidas viejas tenían 55 de radiación "virtual" durante todo su historial
            db.execSQL("UPDATE partidas SET sumaRadiacion = 55 * minutosBienestar");
        }
    };

    public static BaseDatos get(Context ctx) {
        if (instancia == null) {
            synchronized (BaseDatos.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(ctx.getApplicationContext(), BaseDatos.class, "godzipet.db")
                            .addMigrations(MIGRACION_1_2)
                            .build();
                }
            }
        }
        return instancia;
    }
}
