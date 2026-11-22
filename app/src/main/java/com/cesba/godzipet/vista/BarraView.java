package com.cesba.godzipet.vista;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Barra segmentada estilo LCD retro con contorno grueso. Parpadea en rojo si es crítica. */
public class BarraView extends View {
    private static final int SEGMENTOS = 20;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private int valor;
    private int maximo = 100;
    private int color = 0xFF7CFF6B;
    private int critico = 20;

    public BarraView(Context c) { this(c, null); }

    public BarraView(Context c, AttributeSet a) {
        super(c, a);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void configurar(int color, int critico) {
        this.color = color;
        this.critico = critico;
        invalidate();
    }

    public void setValor(int v, int max) {
        valor = Math.max(0, Math.min(max, v));
        maximo = Math.max(1, max);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float d = getResources().getDisplayMetrics().density;
        r.set(0, 0, w, h);
        p.setColor(0xFF070A16);
        c.drawRoundRect(r, h / 2.5f, h / 2.5f, p);
        r.inset(3 * d, 3 * d);
        p.setColor(0xFF0B1124);
        c.drawRoundRect(r, h / 3f, h / 3f, p);
        boolean alerta = valor < critico && (System.currentTimeMillis() / 400) % 2 == 0;
        int col = alerta ? 0xFFFF4D5E : color;
        int llenos = Math.round(valor / (float) maximo * SEGMENTOS);
        if (valor > 0 && llenos == 0) llenos = 1;
        float gap = 2 * d;
        float sw = (r.width() - gap * (SEGMENTOS + 1)) / SEGMENTOS;
        for (int i = 0; i < SEGMENTOS; i++) {
            float x = r.left + gap + i * (sw + gap);
            p.setColor(i < llenos ? col : 0xFF1A2342);
            c.drawRoundRect(x, r.top + gap, x + sw, r.bottom - gap, 2 * d, 2 * d, p);
            if (i < llenos) {
                p.setColor(Color.argb(70, 255, 255, 255));
                c.drawRect(x, r.top + gap, x + sw, r.top + gap + (r.height() - 2 * gap) * 0.35f, p);
            }
        }
        if (valor < critico) postInvalidateDelayed(200);
    }
}
