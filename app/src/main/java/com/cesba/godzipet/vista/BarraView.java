package com.cesba.godzipet.vista;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Barra segmentada estilo LCD retro con contorno grueso. Anima los cambios de valor y, si se
 * define una zona ideal (barra de radiación), la resalta y dibuja un marcador del valor actual.
 */
public class BarraView extends View {
    private static final int SEGMENTOS = 20;
    private static final long DURACION_MS = 350;
    private static final int COL_CIAN = 0xFF3DE0FF;
    private static final int COL_ROJO = 0xFFFF4D5E;
    private static final int COL_GRIS = 0xFF6B7390;
    private static final int COL_AMBAR = 0xFFFFB02E;
    private static final int UMBRAL_PELIGRO = 85;
    private static final int UMBRAL_BAJO = 20;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path flecha = new Path();
    private int valor;
    private int maximo = 100;
    private int color = 0xFF7CFF6B;
    private int critico = 20;

    // Animación
    private float mostrado;
    private float desde;
    private long inicioAnim;
    private boolean animando;
    private boolean primerValor = true;

    // Zona ideal (-1 = sin zona)
    private int zonaMin = -1, zonaMax = -1;

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

    /** Define la zona ideal [min, max] (en las mismas unidades que el valor); activa el modo radiación. */
    public void setZona(int min, int max) {
        zonaMin = Math.min(min, max);
        zonaMax = Math.max(min, max);
        invalidate();
    }

    public void setValor(int v, int max) {
        int nuevo = Math.max(0, Math.min(max, v));
        maximo = Math.max(1, max);
        if (primerValor) {
            primerValor = false;
            valor = nuevo;
            mostrado = nuevo;
            animando = false;
        } else if (nuevo != valor) {
            desde = mostrado;
            valor = nuevo;
            inicioAnim = System.nanoTime();
            animando = true;
        }
        invalidate();
    }

    private boolean hayZona() { return zonaMin >= 0; }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float d = getResources().getDisplayMetrics().density;

        if (animando) {
            float f = (System.nanoTime() - inicioAnim) / (DURACION_MS * 1e6f);
            if (f >= 1f) {
                mostrado = valor;
                animando = false;
            } else {
                float inv = 1f - f;
                mostrado = desde + (valor - desde) * (1f - inv * inv * inv);
            }
        }

        r.set(0, 0, w, h);
        p.setColor(0xFF070A16);
        c.drawRoundRect(r, h / 2.5f, h / 2.5f, p);
        r.inset(3 * d, 3 * d);
        p.setColor(0xFF0B1124);
        c.drawRoundRect(r, h / 3f, h / 3f, p);

        boolean parpadeo = (System.currentTimeMillis() / 400) % 2 == 0;
        int col = colorRelleno(parpadeo);
        int llenos = Math.round(mostrado / maximo * SEGMENTOS);
        if (mostrado > 0.5f && llenos == 0) llenos = 1;
        float gap = 2 * d;
        float sw = (r.width() - gap * (SEGMENTOS + 1)) / SEGMENTOS;
        for (int i = 0; i < SEGMENTOS; i++) {
            float x = r.left + gap + i * (sw + gap);
            boolean enZona = hayZona() && segmentoEnZona(i);
            p.setColor(i < llenos ? col : (enZona ? 0xFF1F4F5E : 0xFF1A2342));
            c.drawRoundRect(x, r.top + gap, x + sw, r.bottom - gap, 2 * d, 2 * d, p);
            if (i < llenos) {
                p.setColor(Color.argb(70, 255, 255, 255));
                c.drawRect(x, r.top + gap, x + sw, r.top + gap + (r.height() - 2 * gap) * 0.35f, p);
            }
        }
        if (hayZona()) dibujarZona(c, gap, d);

        if (animando || (!hayZona() && valor < critico) || (hayZona() && valor >= UMBRAL_PELIGRO)) {
            if (animando) postInvalidateOnAnimation(); else postInvalidateDelayed(200);
        }
    }

    private int colorRelleno(boolean parpadeo) {
        if (!hayZona()) {
            return (mostrado < critico && parpadeo) ? COL_ROJO : color;
        }
        if (mostrado >= UMBRAL_PELIGRO) return parpadeo ? COL_ROJO : 0xFF9A2A38;
        if (mostrado >= zonaMin && mostrado <= zonaMax) return COL_CIAN;
        if (mostrado < UMBRAL_BAJO) return COL_GRIS;
        return COL_AMBAR;
    }

    /** Un segmento está en la zona si su rango de valores intersecta [zonaMin, zonaMax]. */
    private boolean segmentoEnZona(int i) {
        float ini = i * (float) maximo / SEGMENTOS, fin = (i + 1) * (float) maximo / SEGMENTOS;
        return fin > zonaMin && ini < zonaMax;
    }

    private void dibujarZona(Canvas c, float gap, float d) {
        float ancho = r.width() - gap * 2;
        float x0 = r.left + gap + ancho * zonaMin / maximo;
        float x1 = r.left + gap + ancho * zonaMax / maximo;
        // Marco de la zona ideal
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.5f * d);
        p.setColor(0xCC3DE0FF);
        c.drawRect(x0, r.top + 0.5f * d, x1, r.bottom - 0.5f * d, p);
        p.setStyle(Paint.Style.FILL);
        // Marcador triangular en el valor actual
        float mx = r.left + gap + ancho * mostrado / maximo;
        float tam = Math.max(4 * d, r.height() * 0.3f);
        flecha.reset();
        flecha.moveTo(mx - tam * 0.7f, r.top);
        flecha.lineTo(mx + tam * 0.7f, r.top);
        flecha.lineTo(mx, r.top + tam);
        flecha.close();
        p.setColor(0xFF070A16);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f * d);
        c.drawPath(flecha, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFFFFFFF);
        c.drawPath(flecha, p);
    }
}
