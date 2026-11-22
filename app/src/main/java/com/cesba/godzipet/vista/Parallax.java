package com.cesba.godzipet.vista;

import android.content.Context;
import android.graphics.Canvas;

import com.cesba.godzipet.modelo.Escenario;

/**
 * CONTRATO (lo implementa el agente del motor 2.5D): fondos por capas pre-renderizadas en Blender
 * (assets/escenas/&lt;escenario&gt;/&lt;dia|noche&gt;_&lt;0-4&gt;.webp, ver docs/ESPEC_CAPAS.md) con
 * desplazamiento parallax según la inclinación del teléfono.
 * Capas 0-3 = fondo (detrás del kaiju); capa 4 = primer plano (delante del kaiju).
 */
public final class Parallax {
    public Parallax(Context c) { }

    /** true si existen las capas de ese escenario en assets. */
    public boolean disponible(Escenario e) { return false; }

    /** Prepara (carga/escala y cachea) las capas para el tamaño de la vista. Llamar desde onSizeChanged o al cambiar de escenario. */
    public void preparar(Escenario e, int anchoVista, int altoVista) { }

    /**
     * Dibuja las capas 0-3. incl* en [-1,1] (giroscopio); t en segundos; noche 0..1 mezcla las
     * versiones día/noche; atardecer 0..1 aplica un tinte cálido.
     */
    public void dibujarFondo(Canvas c, float w, float h, float inclX, float inclY, float t, float noche, float atardecer) { }

    /** Dibuja la capa 4 (primer plano) por delante del kaiju. */
    public void dibujarFrente(Canvas c, float w, float h, float inclX, float inclY, float t, float noche) { }

    public void liberar() { }
}
