package com.cesba.godzipet.datos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class JuegoDao {
    // ---- usuarios
    @Insert(onConflict = OnConflictStrategy.ABORT)
    public abstract long insertarUsuario(UsuarioEntity u);

    @Query("SELECT * FROM usuarios WHERE usuario = :usuario LIMIT 1")
    public abstract UsuarioEntity buscarUsuario(String usuario);

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    public abstract UsuarioEntity usuarioPorId(long id);

    // ---- partida
    @Query("SELECT * FROM partidas WHERE usuarioId = :uid LIMIT 1")
    public abstract PartidaEntity partida(long uid);

    @Query("SELECT * FROM inventario WHERE usuarioId = :uid")
    public abstract List<InventarioEntity> inventario(long uid);

    @Query("SELECT * FROM decoraciones WHERE usuarioId = :uid")
    public abstract List<DecoracionEntity> decoraciones(long uid);

    @Query("SELECT * FROM misiones WHERE usuarioId = :uid")
    public abstract List<MisionEntity> misiones(long uid);

    @Query("SELECT * FROM records WHERE usuarioId = :uid")
    public abstract List<RecordEntity> records(long uid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void guardarPartida(PartidaEntity p);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void guardarInventario(List<InventarioEntity> l);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void guardarDecoraciones(List<DecoracionEntity> l);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void guardarMisiones(List<MisionEntity> l);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void guardarRecords(List<RecordEntity> l);

    @Query("DELETE FROM partidas WHERE usuarioId = :uid")
    abstract void borrarPartida(long uid);

    @Query("DELETE FROM inventario WHERE usuarioId = :uid")
    abstract void borrarInventario(long uid);

    @Query("DELETE FROM decoraciones WHERE usuarioId = :uid")
    abstract void borrarDecoraciones(long uid);

    @Query("DELETE FROM misiones WHERE usuarioId = :uid")
    abstract void borrarMisiones(long uid);

    @Query("DELETE FROM records WHERE usuarioId = :uid")
    abstract void borrarRecords(long uid);

    /** Guarda la partida completa de forma atómica (todo o nada). */
    @Transaction
    public void guardarTodo(Instantanea s) {
        guardarPartida(s.partida);
        borrarInventario(s.partida.usuarioId);
        guardarInventario(s.inventario);
        borrarDecoraciones(s.partida.usuarioId);
        guardarDecoraciones(s.decoraciones);
        guardarMisiones(s.misiones);
        guardarRecords(s.records);
    }

    @Transaction
    public void borrarTodo(long uid) {
        borrarPartida(uid);
        borrarInventario(uid);
        borrarDecoraciones(uid);
        borrarMisiones(uid);
        borrarRecords(uid);
    }
}
