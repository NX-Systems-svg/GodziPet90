package com.cesba.godzipet.datos;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Cuenta local (sin internet). La contraseña se guarda como hash PBKDF2 + sal. */
@Entity(tableName = "usuarios", indices = {@Index(value = "usuario", unique = true)})
public class UsuarioEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public String usuario = "";
    @NonNull
    public String sal = "";
    @NonNull
    public String hash = "";
    public long creado;
}
