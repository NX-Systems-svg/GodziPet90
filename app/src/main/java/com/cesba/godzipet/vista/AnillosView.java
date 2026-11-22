package com.cesba.godzipet.vista;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/** CONTRATO (lo implementa el agente de minijuegos): minijuego "Anillos de Humo" (homenaje a Minilla). */
public class AnillosView extends View {
    public static final int DURACION_MS = 30_000;
    public static final int META_ACIERTOS = 10;
    public static final int PUNTOS_POR_ACIERTO = 10;

    public interface Oyente {
        void progreso(int aciertos, int fallos, int segundosRestantes);
        void fin(int aciertos, int puntaje);
        void acierto();
    }

    public AnillosView(Context c) { this(c, null); }

    public AnillosView(Context c, AttributeSet a) { super(c, a); }

    public void setApariencia(Apariencia a) { }
    public void setOyente(Oyente o) { }
    public boolean isJugando() { return false; }
    public void iniciar() { }
    public void detener() { }
}
