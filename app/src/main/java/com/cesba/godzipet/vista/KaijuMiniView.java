package com.cesba.godzipet.vista;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

/** Dibuja sólo al kaiju (sin escenario): logo, selección de huevo, inicio y progreso. */
public class KaijuMiniView extends View {
    private final KaijuPainter kaiju = new KaijuPainter();
    private final long inicio = System.currentTimeMillis();
    private Apariencia apariencia;
    private boolean silueta;

    public KaijuMiniView(Context c) { this(c, null); }

    public KaijuMiniView(Context c, AttributeSet a) { super(c, a); }

    public void setApariencia(Apariencia a) {
        apariencia = a;
        invalidate();
    }

    public void setSilueta(boolean s) {
        silueta = s;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (apariencia == null) return;
        float w = getWidth(), h = getHeight();
        float tam = Math.min(h * 0.9f / Math.max(0.62f, apariencia.escalaEtapa()) * 0.95f, w * 0.85f);
        if (apariencia.etapa == com.cesba.godzipet.modelo.Etapa.HUEVO) tam = Math.min(h * 1.9f, w * 2.6f);
        float u = tam / 100f * apariencia.escalaEtapa();
        float t = (System.currentTimeMillis() - inicio) / 1000f;
        float desplaza = apariencia.etapa == com.cesba.godzipet.modelo.Etapa.HUEVO ? 0 : 8 * u;
        kaiju.dibujar(c, w / 2f + desplaza, h * 0.96f, tam, apariencia, t, 0, silueta);
        postInvalidateDelayed(33);
    }
}
