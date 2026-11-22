package com.cesba.godzipet.datos;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Fila principal de la partida de un usuario: personaje, estadísticas, economía y progreso. */
@Entity(tableName = "partidas")
public class PartidaEntity {
    @PrimaryKey
    public long usuarioId;
    // Personaje
    @NonNull public String nombre = "";
    @NonNull public String origen = "";
    @NonNull public String etapa = "";
    @NonNull public String ruta = "";
    public boolean dormido;
    public long despertarEn;
    public int toquesHuevo;
    // Necesidades
    public double alimentacion, felicidad, energia, higiene, salud;
    // Economía y progreso
    public int monedas;
    public int xp;
    public double cargaAliento;
    public long ultimaActualizacion;
    public boolean evolucionPendiente;
    // Alerta Kaiju
    public String alertaMonstruo;
    public String alertaLugar;
    public long alertaExpira;
    public long proximaAlerta;
    // Tiempos de espera
    public long ultimoJugar, ultimoEntrenar, ultimoBanoGratis;
    // Contadores (historial de hábitos)
    public int cAlimentar, cJugar, cDormir, cBanar, cCurar, cEntrenar;
    public int cMinijuegos, cVictorias, cDefensas, cAlientos;
    public double sumaBienestar, minutosBienestar;
    @NonNull public String diaClave = "";
    public int hoyAlimentar, hoyMinijuegos, hoyEntrenar;

    // ---- Versión 2 (migración 1→2): radiación, fusión, generaciones, escenarios y secretos
    public double radiacion;
    public double fusion;
    public int generacion;
    public String escenario;
    public long ultimaCentral;
    public int cCentrales;
    public double sumaRadiacion;
    public int entrenosBase, cuidadosBase;
    public double hoyMinutosZona;
    /** Listas separadas por comas (Kaiju-pedia). */
    public String secretos, visitados, formas;
}
