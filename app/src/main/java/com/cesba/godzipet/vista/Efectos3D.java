package com.cesba.godzipet.vista;

import android.view.View;

/**
 * CONTRATO (lo implementa el agente del motor 2.5D): efectos de GPU con shaders AGSL
 * (Android 13+, API 33) aplicados a toda la vista con RenderEffect. En versiones anteriores
 * {@link #aplicar} no hace nada.
 */
public final class Efectos3D {
    /** Parámetros del cuadro actual (se reutiliza el mismo objeto). */
    public static final class Params {
        /** Distorsión por calor 0..1 (Burning / fusión). */
        public float calor;
        /** Onda de choque: centro en px y progreso 0..1 (o -1 si no hay). */
        public float choqueX, choqueY, choqueProgreso = -1;
        /** Onda en el agua: centro en px y progreso 0..1 (o -1). */
        public float ondaX, ondaY, ondaProgreso = -1;
        /** Rayos de luz volumétricos 0..1 desde (solX, solY) px. */
        public float rayos, solX, solY;
        /** Resplandor (bloom) 0..1 de las zonas brillantes. */
        public float bloom;
        /** Viñeta cinematográfica 0..1. */
        public float vineta;
    }

    public static boolean soportado() {
        return android.os.Build.VERSION.SDK_INT >= 33;
    }

    /** Aplica o actualiza el efecto combinado sobre la vista (llamar una vez por cuadro). */
    public void aplicar(View v, float t, Params p) { }

    /** Quita cualquier efecto de la vista. */
    public void quitar(View v) { }
}
